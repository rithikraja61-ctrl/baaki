package com.baaki.service;

import com.baaki.domain.Customer;
import com.baaki.domain.Invoice;
import com.baaki.domain.PaymentHistory;
import com.baaki.money.GstinValidator;
import com.baaki.money.IndianStates;
import com.baaki.repo.CustomerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class CustomerService {
    private final CustomerRepository repo;
    private final SnapshotService snapshots;

    public CustomerService(CustomerRepository repo, SnapshotService snapshots) {
        this.repo = repo; this.snapshots = snapshots;
    }

    public record CustomerView(Long id, String name, String contactPerson, String phone, String email, String address,
                               String city, String stateCode, String stateName, String gstin, boolean registered,
                               int creditDays, long owedPaise, long overduePaise, int openInvoices,
                               ReliabilityCalculator.Stats reliability, String preferredLanguage) {}

    public static Map<Long, ReliabilityCalculator.Stats> reliability(Snapshot s) {
        Map<Long, List<Integer>> late = new HashMap<>();
        for (PaymentHistory h : s.history)
            late.computeIfAbsent(h.customerId, k -> new ArrayList<>()).add((int) ChronoUnit.DAYS.between(h.dueDate, h.paidDate));
        for (Invoice i : s.invoices) {
            if (!"PAID".equals(i.status) && !(s.outstanding(i) == 0 && !"DRAFT".equals(i.status))) continue;
            var d = s.lastPaymentDate.get(i.id);
            if (d != null) late.computeIfAbsent(i.customerId, k -> new ArrayList<>()).add((int) ChronoUnit.DAYS.between(i.dueDate, d));
        }
        // still-unpaid overdue invoices also count as late, so a customer who has stopped paying drops in grade
        for (Invoice i : s.invoices) {
            if (s.isOpen(i) && s.daysOverdue(i) > 0)
                late.computeIfAbsent(i.customerId, k -> new ArrayList<>()).add(s.daysOverdue(i));
        }
        Map<Long, ReliabilityCalculator.Stats> out = new HashMap<>();
        for (Long cid : s.customers.keySet()) out.put(cid, ReliabilityCalculator.compute(late.getOrDefault(cid, List.of())));
        return out;
    }

    public List<CustomerView> list() {
        Snapshot s = snapshots.load();
        var rel = reliability(s);
        List<CustomerView> out = new ArrayList<>();
        for (Customer c : s.customers.values()) out.add(view(s, c, rel.get(c.id)));
        out.sort(Comparator.comparing(CustomerView::name));
        return out;
    }

    public CustomerView get(Long id) {
        Snapshot s = snapshots.load();
        Customer c = Optional.ofNullable(s.customers.get(id)).orElseThrow(() -> new NoSuchElementException("Customer not found"));
        return view(s, c, reliability(s).get(id));
    }

    private CustomerView view(Snapshot s, Customer c, ReliabilityCalculator.Stats st) {
        long owed = 0, overdue = 0; int open = 0;
        for (Invoice i : s.invoices) {
            if (!i.customerId.equals(c.id) || !s.isOpen(i)) continue;
            owed += s.outstanding(i); open++;
            if (s.today.isAfter(i.dueDate)) overdue += s.outstanding(i);
        }
        return new CustomerView(c.id, c.name, c.contactPerson, c.phone, c.email, c.address, c.city, c.stateCode,
                IndianStates.name(c.stateCode), c.gstin, c.gstin != null, c.creditDays, owed, overdue, open, st, com.baaki.domain.Languages.orEnglish(c.preferredLanguage));
    }

    /** One past payment: what was due, when it came in, and how late. */
    public record PaymentRecord(String reference, java.time.LocalDate dueDate, java.time.LocalDate paidDate,
                                long amountPaise, int daysLate, boolean unpaid) {}

    /** Payment record, newest first: older history, invoices paid in Baaki, and invoices still unpaid past due. */
    public List<PaymentRecord> paymentRecord(Long id) {
        Snapshot s = snapshots.load();
        if (!s.customers.containsKey(id)) throw new NoSuchElementException("Customer not found");
        List<PaymentRecord> out = new ArrayList<>();
        for (PaymentHistory h : s.history)
            if (h.customerId.equals(id)) out.add(new PaymentRecord(h.reference, h.dueDate, h.paidDate, h.amountPaise,
                    (int) ChronoUnit.DAYS.between(h.dueDate, h.paidDate), false));
        for (Invoice i : s.invoices) {
            if (!i.customerId.equals(id) || "DRAFT".equals(i.status)) continue;
            var d = s.lastPaymentDate.get(i.id);
            if (s.outstanding(i) == 0 && d != null)
                out.add(new PaymentRecord(i.invoiceNumber, i.dueDate, d, i.totalPaise, (int) ChronoUnit.DAYS.between(i.dueDate, d), false));
            else if (s.isOpen(i) && s.daysOverdue(i) > 0)
                out.add(new PaymentRecord(i.invoiceNumber, i.dueDate, null, s.outstanding(i), s.daysOverdue(i), true));
        }
        out.sort(Comparator.comparing((PaymentRecord r) -> r.paidDate() == null ? s.today : r.paidDate()).reversed());
        return out;
    }

    public record CustomerReq(String name, String contactPerson, String phone, String email, String address,
                              String city, String stateCode, String gstin, Integer creditDays, String preferredLanguage) {}

    @Transactional
    public CustomerView save(Long id, CustomerReq r) {
        if (r.name() == null || r.name().isBlank()) throw new IllegalArgumentException("Customer name is required");
        Customer c = id == null ? new Customer() : repo.findById(id).orElseThrow(() -> new NoSuchElementException("Customer not found"));
        c.name = r.name().trim();
        c.contactPerson = r.contactPerson(); c.phone = r.phone(); c.email = r.email();
        c.address = r.address(); c.city = r.city();
        c.creditDays = r.creditDays() == null ? 30 : r.creditDays();
        if (r.preferredLanguage() != null && !com.baaki.domain.Languages.ALL.contains(r.preferredLanguage()))
            throw new IllegalArgumentException("Unsupported language");
        c.preferredLanguage = com.baaki.domain.Languages.orEnglish(r.preferredLanguage() == null ? c.preferredLanguage : r.preferredLanguage());
        if (c.creditDays < 0 || c.creditDays > 365) throw new IllegalArgumentException("Credit days must be between 0 and 365");
        String g = r.gstin() == null ? "" : r.gstin().trim().toUpperCase();
        if (!g.isEmpty()) {
            if (!GstinValidator.isValid(g)) throw new IllegalArgumentException("GSTIN is not valid (check the 15 characters and the last check digit)");
            c.gstin = g;
            c.stateCode = GstinValidator.stateCode(g);
        } else {
            c.gstin = null;
            if (r.stateCode() == null || !IndianStates.BY_CODE.containsKey(r.stateCode()))
                throw new IllegalArgumentException("Pick the customer's state");
            c.stateCode = r.stateCode();
        }
        repo.save(c);
        return get(c.id);
    }
}

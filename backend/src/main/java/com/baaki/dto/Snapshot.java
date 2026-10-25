package com.baaki.dto;

import com.baaki.model.*;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/** An in-memory view of all data for one request. The demo data set is small, so this keeps logic simple. */
public class Snapshot {
    public final LocalDate today;
    public final BusinessProfile profile;
    public final Map<Long, Customer> customers;
    public final List<Invoice> invoices;
    public final Map<Long, Long> paidByInvoice = new HashMap<>();
    public final Map<Long, Long> creditedByInvoice = new HashMap<>();
    public final Map<Long, LocalDate> lastPaymentDate = new HashMap<>();
    public final Map<Long, List<InvoiceEvent>> eventsByInvoice;
    public final Set<Long> invoicesWithOpenInbox;
    public final List<Expense> expenses;
    public final List<PaymentHistory> history;
    public final List<LadderStage> ladder;

    public Snapshot(LocalDate today, BusinessProfile profile, List<Customer> customers, List<Invoice> invoices,
                    List<PaymentAllocation> allocations, Map<Long, Payment> payments, List<CreditNote> creditNotes,
                    List<InvoiceEvent> events, List<InboxItem> openInbox, List<Expense> expenses,
                    List<PaymentHistory> history, List<LadderStage> ladder) {
        this.today = today;
        this.profile = profile;
        this.customers = customers.stream().collect(Collectors.toMap(c -> c.id, c -> c, (a, b) -> a, LinkedHashMap::new));
        this.invoices = invoices;
        for (PaymentAllocation a : allocations) {
            Payment p = payments.get(a.paymentId);
            if (p == null || !"CONFIRMED".equals(p.status)) continue;
            paidByInvoice.merge(a.invoiceId, a.amountPaise, Long::sum);
            lastPaymentDate.merge(a.invoiceId, p.paymentDate, (x, y) -> x.isAfter(y) ? x : y);
        }
        for (CreditNote cn : creditNotes) creditedByInvoice.merge(cn.invoiceId, cn.amountPaise, Long::sum);
        this.eventsByInvoice = events.stream().collect(Collectors.groupingBy(e -> e.invoiceId));
        this.invoicesWithOpenInbox = openInbox.stream().map(i -> i.invoiceId).collect(Collectors.toSet());
        this.expenses = expenses;
        this.history = history;
        this.ladder = ladder;
    }

    public long paid(Invoice i) { return paidByInvoice.getOrDefault(i.id, 0L); }
    public long credited(Invoice i) { return creditedByInvoice.getOrDefault(i.id, 0L); }
    public long outstanding(Invoice i) { return Math.max(0, i.totalPaise - paid(i) - credited(i)); }
    public Customer customer(Invoice i) { return customers.get(i.customerId); }

    /** Sent, not paid, money still owed. */
    public boolean isOpen(Invoice i) {
        return !"DRAFT".equals(i.status) && !"PAID".equals(i.status) && outstanding(i) > 0;
    }

    public int daysOverdue(Invoice i) {
        long d = java.time.temporal.ChronoUnit.DAYS.between(i.dueDate, today);
        return (int) Math.max(0, d);
    }

    /** draft, sent, due soon, overdue, part paid, paid, disputed */
    public String displayStatus(Invoice i) {
        if ("DRAFT".equals(i.status)) return "DRAFT";
        if ("PAID".equals(i.status) || outstanding(i) == 0) return "PAID";
        if ("DISPUTED".equals(i.status)) return "DISPUTED";
        if (paid(i) > 0 || credited(i) > 0) return "PART_PAID";
        if (today.isAfter(i.dueDate)) return "OVERDUE";
        if (!i.dueDate.isAfter(today.plusDays(7))) return "DUE_SOON";
        return "SENT";
    }

    public boolean isMicroOrSmall() {
        return profile.udyamRegistered && ("MICRO".equals(profile.msmeCategory) || "SMALL".equals(profile.msmeCategory));
    }
}

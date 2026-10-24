package com.baaki.service;

import com.baaki.config.BaakiProperties;
import com.baaki.domain.*;
import com.baaki.money.GstCalculator;
import com.baaki.money.IndianNumberWords;
import com.baaki.money.IndianStates;
import com.baaki.repo.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class InvoiceService {
    private static final SecureRandom RND = new SecureRandom();
    private final InvoiceRepository invoices;
    private final InvoiceLineRepository lines;
    private final CustomerRepository customers;
    private final BusinessProfileRepository profiles;
    private final InvoiceEventRepository events;
    private final PaymentAllocationRepository allocations;
    private final PaymentRepository payments;
    private final CreditNoteRepository creditNotes;
    private final InboxItemRepository inbox;
    private final InvoiceNumberService numbers;
    private final SnapshotService snapshots;
    private final BaakiProperties props;
    private final Clock clock;

    public InvoiceService(InvoiceRepository invoices, InvoiceLineRepository lines, CustomerRepository customers,
                          BusinessProfileRepository profiles, InvoiceEventRepository events,
                          PaymentAllocationRepository allocations, PaymentRepository payments,
                          CreditNoteRepository creditNotes, InboxItemRepository inbox, InvoiceNumberService numbers,
                          SnapshotService snapshots, BaakiProperties props, Clock clock) {
        this.invoices = invoices; this.lines = lines; this.customers = customers; this.profiles = profiles;
        this.events = events; this.allocations = allocations; this.payments = payments; this.creditNotes = creditNotes;
        this.inbox = inbox; this.numbers = numbers; this.snapshots = snapshots; this.props = props; this.clock = clock;
    }

    // ---------- views ----------
    public record InvoiceSummary(Long id, String invoiceNumber, Long customerId, String customerName,
                                 LocalDate invoiceDate, LocalDate dueDate, String status, long totalPaise,
                                 long paidPaise, long outstandingPaise, int daysOverdue, boolean paused, String payToken) {}

    public static InvoiceSummary summary(Snapshot s, Invoice i) {
        Customer c = s.customer(i);
        boolean paused = "DISPUTED".equals(i.status) || s.invoicesWithOpenInbox.contains(i.id)
                || (i.promisedDate != null && !i.promisedDate.isBefore(s.today))
                || (i.snoozedUntil != null && !i.snoozedUntil.isBefore(s.today));
        int od = s.isOpen(i) ? s.daysOverdue(i) : 0;
        return new InvoiceSummary(i.id, i.invoiceNumber, i.customerId, c == null ? "" : c.name, i.invoiceDate,
                i.dueDate, s.displayStatus(i), i.totalPaise, s.paid(i) + s.credited(i), s.isOpen(i) ? s.outstanding(i) : 0,
                od, paused, i.payToken);
    }

    public List<InvoiceSummary> list() {
        Snapshot s = snapshots.load();
        List<InvoiceSummary> out = new ArrayList<>();
        for (Invoice i : s.invoices) out.add(summary(s, i));
        out.sort(Comparator.comparing(InvoiceSummary::invoiceDate).reversed().thenComparing(InvoiceSummary::id, Comparator.reverseOrder()));
        return out;
    }

    public record Detail(InvoiceSummary summary, Invoice invoice, List<InvoiceLine> lines, Customer customer,
                         BusinessProfile business, String placeOfSupplyName, boolean intraState, String amountInWords,
                         boolean eInvoiceRequired, String docTitle, List<InvoiceEvent> timeline,
                         List<Map<String, Object>> payments, List<CreditNote> creditNotes, List<InboxItem> inbox) {}

    public Detail detail(Long id) {
        Snapshot s = snapshots.load();
        Invoice i = s.invoices.stream().filter(x -> x.id.equals(id)).findFirst()
                .orElseThrow(() -> new NoSuchElementException("Invoice not found"));
        return detail(s, i);
    }

    public Detail detailByToken(String token) {
        Snapshot s = snapshots.load();
        Invoice i = s.invoices.stream().filter(x -> x.payToken.equals(token) && !"DRAFT".equals(x.status)).findFirst()
                .orElseThrow(() -> new NoSuchElementException("This payment link is not valid"));
        return detail(s, i);
    }

    private Detail detail(Snapshot s, Invoice i) {
        List<Map<String, Object>> pays = new ArrayList<>();
        for (PaymentAllocation a : allocations.findByInvoiceId(i.id)) {
            payments.findById(a.paymentId).ifPresent(p -> {
                if (!"CONFIRMED".equals(p.status)) return;
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("paymentId", p.id); m.put("date", p.paymentDate); m.put("amountPaise", a.amountPaise);
                m.put("reference", p.reference); m.put("source", p.source);
                pays.add(m);
            });
        }
        String title = switch (i.docType) {
            case "BILL_OF_SUPPLY" -> "Bill of Supply";
            case "INVOICE" -> "Invoice";
            default -> "Tax Invoice";
        };
        List<InboxItem> items = new ArrayList<>(inbox.findAll().stream().filter(x -> x.invoiceId.equals(i.id)).toList());
        return new Detail(summary(s, i), i, lines.findByInvoiceIdOrderByLineNo(i.id), s.customer(i), s.profile,
                IndianStates.name(i.placeOfSupply), GstCalculator.isIntraState(s.profile.stateCode, i.placeOfSupply),
                IndianNumberWords.rupees(i.totalPaise), s.profile.turnoverPaise > props.eInvoiceThresholdPaise(), title,
                events.findByInvoiceIdOrderByEventAt(i.id), pays, creditNotes.findByInvoiceId(i.id), items);
    }

    // ---------- create / edit ----------
    public record LineReq(String description, String hsnSac, BigDecimal quantity, String unit, Long ratePaise,
                          Long discountPaise, Integer gstRateBp) {}

    public record InvoiceReq(Long customerId, LocalDate invoiceDate, String placeOfSupply, Boolean reverseCharge,
                             String deliveryAddress, String notes, List<LineReq> lines, Boolean send) {}

    public static GstCalculator.DocType docTypeFor(String registrationType) {
        return switch (registrationType == null ? "REGULAR" : registrationType) {
            case "COMPOSITION" -> GstCalculator.DocType.BILL_OF_SUPPLY;
            case "UNREGISTERED" -> GstCalculator.DocType.INVOICE;
            default -> GstCalculator.DocType.TAX_INVOICE;
        };
    }

    /** Live totals for the New invoice screen; same maths as saving. */
    public GstCalculator.Result preview(InvoiceReq r) {
        BusinessProfile p = profiles.findById(1L).orElseThrow();
        String pos = resolvePlace(r);
        return GstCalculator.compute(toInputs(r, p), p.stateCode, pos, docTypeFor(p.registrationType));
    }

    private String resolvePlace(InvoiceReq r) {
        if (r.placeOfSupply() != null && !r.placeOfSupply().isBlank()) return r.placeOfSupply();
        if (r.customerId() == null) throw new IllegalArgumentException("Pick a customer");
        return customers.findById(r.customerId()).orElseThrow(() -> new NoSuchElementException("Customer not found")).stateCode;
    }

    private List<GstCalculator.LineInput> toInputs(InvoiceReq r, BusinessProfile p) {
        if (r.lines() == null || r.lines().isEmpty()) throw new IllegalArgumentException("Add at least one item");
        List<GstCalculator.LineInput> out = new ArrayList<>();
        int n = 0;
        for (LineReq l : r.lines()) {
            n++;
            if (l.quantity() == null || l.quantity().signum() <= 0) throw new IllegalArgumentException("Item " + n + ": quantity must be more than zero");
            if (l.ratePaise() == null || l.ratePaise() < 0) throw new IllegalArgumentException("Item " + n + ": enter a rate");
            int bp = l.gstRateBp() == null ? 0 : l.gstRateBp();
            if (!props.gstRatesBp().contains(bp)) throw new IllegalArgumentException("Item " + n + ": GST rate not allowed");
            out.add(new GstCalculator.LineInput(l.quantity(), l.ratePaise(), l.discountPaise() == null ? 0 : l.discountPaise(), bp));
        }
        return out;
    }

    private void validateLines(InvoiceReq r, BusinessProfile p) {
        int min = p.turnoverPaise > props.hsnSixDigitThresholdPaise() ? 6 : 4;
        int n = 0;
        for (LineReq l : r.lines()) {
            n++;
            if (l.description() == null || l.description().isBlank()) throw new IllegalArgumentException("Item " + n + ": add a description");
            String h = l.hsnSac() == null ? "" : l.hsnSac().trim();
            if (!h.matches("\\d{" + min + ",8}")) throw new IllegalArgumentException("Item " + n + ": HSN/SAC code needs at least " + min + " digits");
            if (l.unit() == null || l.unit().isBlank()) throw new IllegalArgumentException("Item " + n + ": add a unit");
        }
    }

    @Transactional
    public Detail create(InvoiceReq r) { return save(new Invoice(), r, true); }

    @Transactional
    public Detail update(Long id, InvoiceReq r) {
        Invoice i = invoices.findById(id).orElseThrow(() -> new NoSuchElementException("Invoice not found"));
        if ("PAID".equals(i.status)) throw new IllegalStateException("A paid invoice cannot be edited");
        return save(i, r, false);
    }

    private Detail save(Invoice i, InvoiceReq r, boolean isNew) {
        BusinessProfile p = profiles.findById(1L).orElseThrow();
        if (r.customerId() == null) throw new IllegalArgumentException("Pick a customer");
        Customer c = customers.findById(r.customerId()).orElseThrow(() -> new NoSuchElementException("Customer not found"));
        List<GstCalculator.LineInput> inputs = toInputs(r, p);
        validateLines(r, p);
        String pos = resolvePlace(r);
        if (!IndianStates.BY_CODE.containsKey(pos)) throw new IllegalArgumentException("Place of supply is not a valid state");
        GstCalculator.DocType dt = docTypeFor(p.registrationType);
        GstCalculator.Result res = GstCalculator.compute(inputs, p.stateCode, pos, dt);
        LocalDate today = LocalDate.now(clock);
        boolean wasDraft = isNew || "DRAFT".equals(i.status);
        i.customerId = c.id;
        i.invoiceDate = r.invoiceDate() == null ? today : r.invoiceDate();
        i.dueDate = i.invoiceDate.plusDays(c.creditDays);
        i.placeOfSupply = pos;
        i.docType = dt.name();
        i.reverseCharge = Boolean.TRUE.equals(r.reverseCharge());
        i.deliveryAddress = blankToNull(r.deliveryAddress());
        i.notes = blankToNull(r.notes());
        i.taxablePaise = res.taxablePaise(); i.cgstPaise = res.cgstPaise(); i.sgstPaise = res.sgstPaise();
        i.igstPaise = res.igstPaise(); i.roundOffPaise = res.roundOffPaise(); i.totalPaise = res.totalPaise();
        if (isNew) { i.status = "DRAFT"; i.payToken = token(); }
        invoices.save(i);
        if (!isNew) { lines.deleteByInvoiceId(i.id); lines.flush(); }
        int n = 0;
        for (LineReq l : r.lines()) {
            GstCalculator.LineResult lr = res.lines().get(n);
            InvoiceLine e = new InvoiceLine();
            e.invoiceId = i.id; e.lineNo = ++n; e.description = l.description().trim(); e.hsnSac = l.hsnSac().trim();
            e.quantity = l.quantity(); e.unit = l.unit().trim(); e.ratePaise = l.ratePaise();
            e.discountPaise = l.discountPaise() == null ? 0 : l.discountPaise(); e.gstRateBp = l.gstRateBp() == null ? 0 : l.gstRateBp();
            e.taxablePaise = lr.taxablePaise(); e.cgstPaise = lr.cgstPaise(); e.sgstPaise = lr.sgstPaise();
            e.igstPaise = lr.igstPaise(); e.totalPaise = lr.totalPaise();
            lines.save(e);
        }
        LocalDateTime now = LocalDateTime.now(clock);
        events.save(new InvoiceEvent(i.id, c.id, now, isNew ? "CREATED" : "EDITED", null, null,
                isNew ? "Invoice created" : "Invoice edited"));
        if (wasDraft && Boolean.TRUE.equals(r.send())) send(i, now);
        else if (!wasDraft && i.totalPaise <= paidAndCredited(i.id)) i.status = "PAID";
        invoices.save(i);
        return detail(i.id);
    }

    @Transactional
    public Detail send(Long id) {
        Invoice i = invoices.findById(id).orElseThrow(() -> new NoSuchElementException("Invoice not found"));
        if (!"DRAFT".equals(i.status)) throw new IllegalStateException("Invoice already sent");
        send(i, LocalDateTime.now(clock));
        invoices.save(i);
        return detail(id);
    }

    private void send(Invoice i, LocalDateTime now) {
        i.invoiceNumber = numbers.next("INV", i.invoiceDate);
        i.status = "SENT";
        i.sentAt = now;
        events.save(new InvoiceEvent(i.id, i.customerId, now, "SENT", null, null, "Invoice " + i.invoiceNumber + " sent"));
    }

    @Transactional
    public void deleteDraft(Long id) {
        Invoice i = invoices.findById(id).orElseThrow(() -> new NoSuchElementException("Invoice not found"));
        if (!"DRAFT".equals(i.status)) throw new IllegalStateException("Only drafts can be deleted");
        invoices.delete(i);
    }

    long paidAndCredited(Long invoiceId) {
        long paid = 0;
        for (PaymentAllocation a : allocations.findByInvoiceId(invoiceId)) {
            Payment p = payments.findById(a.paymentId).orElse(null);
            if (p != null && "CONFIRMED".equals(p.status)) paid += a.amountPaise;
        }
        for (CreditNote cn : creditNotes.findByInvoiceId(invoiceId)) paid += cn.amountPaise;
        return paid;
    }

    /** Re-evaluates PAID status after a payment or credit note. */
    @Transactional
    public void refreshPaid(Long invoiceId) {
        Invoice i = invoices.findById(invoiceId).orElseThrow();
        if ("DRAFT".equals(i.status)) return;
        long done = paidAndCredited(invoiceId);
        if (done >= i.totalPaise) i.status = "PAID";
        else if ("PAID".equals(i.status)) i.status = "SENT";
        invoices.save(i);
    }

    private static String blankToNull(String s) { return s == null || s.isBlank() ? null : s.trim(); }

    private static String token() {
        String a = "abcdefghjkmnpqrstuvwxyz23456789";
        StringBuilder sb = new StringBuilder();
        for (int k = 0; k < 16; k++) sb.append(a.charAt(RND.nextInt(a.length())));
        return sb.toString();
    }
}

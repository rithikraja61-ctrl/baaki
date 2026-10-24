package com.baaki.service;

import com.baaki.domain.*;
import com.baaki.repo.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class PaymentService {
    private final PaymentRepository payments;
    private final PaymentAllocationRepository allocations;
    private final InvoiceRepository invoices;
    private final InvoiceEventRepository events;
    private final SnapshotService snapshots;
    private final InvoiceService invoiceService;
    private final Clock clock;

    public PaymentService(PaymentRepository payments, PaymentAllocationRepository allocations, InvoiceRepository invoices,
                          InvoiceEventRepository events, SnapshotService snapshots, InvoiceService invoiceService, Clock clock) {
        this.payments = payments; this.allocations = allocations; this.invoices = invoices; this.events = events;
        this.snapshots = snapshots; this.invoiceService = invoiceService; this.clock = clock;
    }

    public record PaymentView(Long id, LocalDate date, long amountPaise, String reference, String description,
                              String source, String status, String note, PaymentMatcher.Suggestion suggestion,
                              List<PaymentAllocation> allocations) {}

    public record ImportResult(int imported, int skippedDuplicates, int debitsIgnored, List<String> errors) {}

    public List<PaymentMatcher.OpenInvoice> openInvoices(Snapshot s) {
        return s.invoices.stream().filter(s::isOpen)
                .sorted(Comparator.comparing((Invoice i) -> i.dueDate))
                .map(i -> new PaymentMatcher.OpenInvoice(i.id, i.invoiceNumber, i.customerId, s.customer(i).name,
                        s.outstanding(i), i.taxablePaise)).toList();
    }

    public List<PaymentView> list() {
        Snapshot s = snapshots.load();
        var open = openInvoices(s);
        List<PaymentView> out = new ArrayList<>();
        for (Payment p : payments.findByStatusInOrderByPaymentDateDesc(List.of("PENDING", "CONFIRMED", "IGNORED"))) {
            if ("HISTORY".equals(p.source)) continue;
            var sug = "PENDING".equals(p.status) ? PaymentMatcher.match(p.description, p.amountPaise, open) : null;
            out.add(new PaymentView(p.id, p.paymentDate, p.amountPaise, p.reference, p.description, p.source, p.status,
                    p.note, sug, allocations.findByPaymentId(p.id)));
        }
        return out;
    }

    /** CSV columns: Date,Description,Reference,Debit,Credit,Balance (amounts in rupees with 2 decimals). */
    @Transactional
    public ImportResult importCsv(String csv) {
        if (csv == null || csv.isBlank()) throw new IllegalArgumentException("The file is empty");
        Set<String> existing = new HashSet<>();
        payments.findAll().forEach(p -> { if (p.reference != null) existing.add(p.reference); });
        int imported = 0, dup = 0, debits = 0;
        List<String> errors = new ArrayList<>();
        String[] rows = csv.replace("\r", "").split("\n");
        for (int r = 1; r < rows.length; r++) {
            String row = rows[r].trim();
            if (row.isEmpty()) continue;
            try {
                List<String> f = parseCsvRow(row);
                LocalDate d = LocalDate.parse(f.get(0).trim());
                long credit = rupeesToPaise(f.get(4));
                if (credit <= 0) { debits++; continue; }
                String ref = f.get(2).trim();
                if (existing.contains(ref)) { dup++; continue; }
                Payment p = new Payment();
                p.paymentDate = d; p.amountPaise = credit; p.reference = ref; p.description = f.get(1).trim();
                p.source = "CSV"; p.status = "PENDING";
                payments.save(p);
                existing.add(ref);
                imported++;
            } catch (RuntimeException e) {
                errors.add("Row " + (r + 1) + ": " + e.getMessage());
            }
        }
        return new ImportResult(imported, dup, debits, errors);
    }

    public record ManualReq(LocalDate date, Long amountPaise, String reference, String description) {}

    @Transactional
    public Payment addManual(ManualReq r) {
        if (r.amountPaise() == null || r.amountPaise() <= 0) throw new IllegalArgumentException("Enter an amount");
        Payment p = new Payment();
        p.paymentDate = r.date() == null ? LocalDate.now(clock) : r.date();
        p.amountPaise = r.amountPaise(); p.reference = r.reference(); p.description = r.description();
        p.source = "MANUAL"; p.status = "PENDING";
        return payments.save(p);
    }

    public record AllocReq(Long invoiceId, Long amountPaise) {}
    public record ConfirmReq(List<AllocReq> allocations, String note) {}

    /** The owner confirms the match. Only now are invoices updated. */
    @Transactional
    public void confirm(Long paymentId, ConfirmReq req) {
        Payment p = payments.findById(paymentId).orElseThrow(() -> new NoSuchElementException("Payment not found"));
        if (!"PENDING".equals(p.status)) throw new IllegalStateException("Payment already handled");
        Snapshot s = snapshots.load();
        long total = 0;
        List<AllocReq> al = req.allocations() == null ? List.of() : req.allocations();
        for (AllocReq a : al) {
            if (a.amountPaise() == null || a.amountPaise() <= 0) throw new IllegalArgumentException("Allocation must be more than zero");
            Invoice i = s.invoices.stream().filter(x -> x.id.equals(a.invoiceId())).findFirst()
                    .orElseThrow(() -> new NoSuchElementException("Invoice not found"));
            if (a.amountPaise() > s.outstanding(i)) throw new IllegalArgumentException("More than what is owed on " + i.invoiceNumber);
            total += a.amountPaise();
        }
        if (total > p.amountPaise) throw new IllegalArgumentException("Allocations add up to more than the payment");
        LocalDateTime now = LocalDateTime.now(clock);
        for (AllocReq a : al) {
            allocations.save(new PaymentAllocation(p.id, a.invoiceId(), a.amountPaise()));
            Invoice i = invoices.findById(a.invoiceId()).orElseThrow();
            events.save(new InvoiceEvent(i.id, i.customerId, now, "PAYMENT", null, null,
                    "Payment " + (p.reference == null ? "" : p.reference) + " of " + com.baaki.money.Money.format(a.amountPaise()) + " confirmed"
                            + (req.note() == null || req.note().isBlank() ? "" : " · " + req.note())));
            invoiceService.refreshPaid(i.id);
        }
        p.status = "CONFIRMED";
        long left = p.amountPaise - total;
        String note = req.note() == null ? "" : req.note().trim();
        if (left > 0) note = (note.isEmpty() ? "" : note + " · ") + com.baaki.money.Money.format(left) + " not applied to any invoice (advance / to refund)";
        p.note = note.isEmpty() ? null : note;
        payments.save(p);
    }

    @Transactional
    public void ignore(Long paymentId, String note) {
        Payment p = payments.findById(paymentId).orElseThrow(() -> new NoSuchElementException("Payment not found"));
        if (!"PENDING".equals(p.status)) throw new IllegalStateException("Payment already handled");
        p.status = "IGNORED"; p.note = note;
        payments.save(p);
    }

    static long rupeesToPaise(String s) {
        String t = s.trim().replace(",", "");
        if (t.isEmpty()) return 0;
        return new java.math.BigDecimal(t).movePointRight(2).setScale(0, java.math.RoundingMode.HALF_UP).longValueExact();
    }

    static List<String> parseCsvRow(String row) {
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean q = false;
        for (int i = 0; i < row.length(); i++) {
            char c = row.charAt(i);
            if (c == '"') {
                if (q && i + 1 < row.length() && row.charAt(i + 1) == '"') { cur.append('"'); i++; }
                else q = !q;
            } else if (c == ',' && !q) { out.add(cur.toString()); cur.setLength(0); }
            else cur.append(c);
        }
        out.add(cur.toString());
        if (out.size() < 5) throw new IllegalArgumentException("expected 6 columns");
        return out;
    }
}

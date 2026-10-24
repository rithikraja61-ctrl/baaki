package com.baaki.service;

import com.baaki.domain.*;
import com.baaki.money.Money;
import com.baaki.repo.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/** Customer replies from the pay link: disputes and "already paid" claims. */
@Service
public class InboxService {
    private final InboxItemRepository inbox;
    private final InvoiceRepository invoices;
    private final InvoiceEventRepository events;
    private final CreditNoteRepository creditNotes;
    private final PaymentRepository payments;
    private final PaymentAllocationRepository allocations;
    private final InvoiceService invoiceService;
    private final InvoiceNumberService numbers;
    private final SnapshotService snapshots;
    private final Clock clock;

    public InboxService(InboxItemRepository inbox, InvoiceRepository invoices, InvoiceEventRepository events,
                        CreditNoteRepository creditNotes, PaymentRepository payments, PaymentAllocationRepository allocations,
                        InvoiceService invoiceService, InvoiceNumberService numbers, SnapshotService snapshots, Clock clock) {
        this.inbox = inbox; this.invoices = invoices; this.events = events; this.creditNotes = creditNotes;
        this.payments = payments; this.allocations = allocations; this.invoiceService = invoiceService;
        this.numbers = numbers; this.snapshots = snapshots; this.clock = clock;
    }

    public record InboxView(InboxItem item, InvoiceService.InvoiceSummary invoice) {}

    public List<InboxView> list(String status) {
        Snapshot s = snapshots.load();
        List<InboxItem> items = "ALL".equals(status) ? inbox.findAll() : inbox.findByStatusOrderByCreatedAtDesc(status == null ? "OPEN" : status);
        List<InboxView> out = new ArrayList<>();
        for (InboxItem it : items) {
            s.invoices.stream().filter(i -> i.id.equals(it.invoiceId)).findFirst()
                    .ifPresent(i -> out.add(new InboxView(it, InvoiceService.summary(s, i))));
        }
        out.sort(Comparator.comparing((InboxView v) -> v.item().createdAt).reversed());
        return out;
    }

    public long openCount() { return inbox.findByStatusOrderByCreatedAtDesc("OPEN").size(); }

    // ----- customer side (/pay/:token) -----
    public record ClaimReq(String reference, LocalDate paidDate) {}
    public record DisputeReq(String reason) {}

    private Invoice byToken(String token) {
        Invoice i = invoices.findByPayToken(token).orElseThrow(() -> new NoSuchElementException("This payment link is not valid"));
        if ("DRAFT".equals(i.status)) throw new NoSuchElementException("This payment link is not valid");
        return i;
    }

    @Transactional
    public void claimPaid(String token, ClaimReq r) {
        Invoice i = byToken(token);
        if (r.reference() == null || r.reference().isBlank()) throw new IllegalArgumentException("Enter the payment reference number");
        if (r.paidDate() == null) throw new IllegalArgumentException("Enter the date you paid");
        InboxItem it = new InboxItem();
        it.invoiceId = i.id; it.kind = "PAID_CLAIM"; it.reference = r.reference().trim(); it.paidDate = r.paidDate();
        it.status = "OPEN"; it.createdAt = LocalDateTime.now(clock);
        inbox.save(it);
        events.save(new InvoiceEvent(i.id, i.customerId, it.createdAt, "PAID_CLAIM", null, null,
                "Customer says they paid on " + r.paidDate() + ", reference " + it.reference));
    }

    @Transactional
    public void dispute(String token, DisputeReq r) {
        Invoice i = byToken(token);
        if (r.reason() == null || r.reason().isBlank()) throw new IllegalArgumentException("Tell us what is wrong");
        InboxItem it = new InboxItem();
        it.invoiceId = i.id; it.kind = "DISPUTE"; it.reason = r.reason().trim(); it.status = "OPEN";
        it.createdAt = LocalDateTime.now(clock);
        inbox.save(it);
        i.status = "DISPUTED";
        invoices.save(i);
        events.save(new InvoiceEvent(i.id, i.customerId, it.createdAt, "DISPUTE", null, null, "Customer raised a dispute: " + it.reason));
    }

    @Transactional
    public void simulateUpiPayment(String token) {
        Invoice i = byToken(token);
        Snapshot s = snapshots.load();
        long owed = s.outstanding(s.invoices.stream().filter(x -> x.id.equals(i.id)).findFirst().orElseThrow());
        if (owed <= 0) throw new IllegalStateException("Nothing is owed on this invoice");
        // Simulated: shows up as a pending credit for the owner to confirm in Payments.
        Payment p = new Payment();
        p.paymentDate = LocalDate.now(clock); p.amountPaise = owed;
        p.reference = "UPI" + System.currentTimeMillis() % 100000000;
        p.description = "UPI CR " + s.customer(i).name.toUpperCase() + " " + i.invoiceNumber + " (simulated)";
        p.source = "UPI_SIM"; p.status = "PENDING";
        payments.save(p);
        events.save(new InvoiceEvent(i.id, i.customerId, LocalDateTime.now(clock), "PAID_CLAIM", null, "UPI",
                "Customer paid " + Money.format(owed) + " by UPI (simulated), waiting for owner to confirm"));
    }

    // ----- owner side -----
    public record ResolveReq(String action, String reply, Long creditNotePaise) {}

    /**
     * action: CREDIT_NOTE (accept dispute, issue credit note), MARK_PAID (accept claim), REJECT (reply, ladder resumes),
     * RESOLVED_EDITED (owner edited the invoice; ladder resumes).
     */
    @Transactional
    public void resolve(Long id, ResolveReq r) {
        InboxItem it = inbox.findById(id).orElseThrow(() -> new NoSuchElementException("Item not found"));
        if (!"OPEN".equals(it.status)) throw new IllegalStateException("Already resolved");
        Invoice i = invoices.findById(it.invoiceId).orElseThrow();
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDate today = now.toLocalDate();
        Snapshot s = snapshots.load();
        long owed = s.outstanding(s.invoices.stream().filter(x -> x.id.equals(i.id)).findFirst().orElseThrow());
        String action = r.action() == null ? "" : r.action();
        switch (action) {
            case "CREDIT_NOTE" -> {
                long amt = r.creditNotePaise() == null ? 0 : r.creditNotePaise();
                if (amt <= 0 || amt > owed) throw new IllegalArgumentException("Credit note must be between ₹0.01 and what is owed");
                CreditNote cn = new CreditNote();
                cn.noteNumber = numbers.next("CN", today); cn.invoiceId = i.id; cn.noteDate = today; cn.amountPaise = amt;
                cn.reason = it.reason;
                creditNotes.save(cn);
                it.status = "ACCEPTED";
                events.save(new InvoiceEvent(i.id, i.customerId, now, "CREDIT_NOTE", null, null,
                        "Credit note " + cn.noteNumber + " for " + Money.format(amt) + " issued"));
            }
            case "MARK_PAID" -> {
                if (owed <= 0) throw new IllegalStateException("Nothing is owed");
                Payment p = new Payment();
                p.paymentDate = it.paidDate == null ? today : it.paidDate; p.amountPaise = owed;
                p.reference = it.reference; p.description = "Customer-reported payment, accepted by owner";
                p.source = "INBOX"; p.status = "CONFIRMED";
                payments.save(p);
                allocations.save(new PaymentAllocation(p.id, i.id, owed));
                it.status = "ACCEPTED";
                events.save(new InvoiceEvent(i.id, i.customerId, now, "PAYMENT", null, null,
                        "Owner accepted customer's payment " + it.reference + " of " + Money.format(owed)));
            }
            case "REJECT" -> {
                if (r.reply() == null || r.reply().isBlank()) throw new IllegalArgumentException("Write a reply to the customer");
                it.status = "REJECTED";
            }
            case "RESOLVED_EDITED" -> it.status = "ACCEPTED";
            default -> throw new IllegalArgumentException("Unknown action");
        }
        it.reply = r.reply();
        it.resolvedAt = now;
        inbox.save(it);
        if ("DISPUTED".equals(i.status) && inbox.findByInvoiceIdAndStatus(i.id, "OPEN").isEmpty()) {
            i.status = "SENT";
            invoices.save(i);
        }
        events.save(new InvoiceEvent(i.id, i.customerId, now, "RESOLVED", null, null,
                "Inbox item resolved: " + action + (r.reply() == null || r.reply().isBlank() ? "" : " · Reply: " + r.reply())));
        invoiceService.refreshPaid(i.id);
    }
}

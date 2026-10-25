package com.baaki.service.impl;

import com.baaki.service.LadderService;
import com.baaki.service.*;
import com.baaki.model.*;
import com.baaki.repository.*;
import com.baaki.config.*;
import com.baaki.dto.*;
import com.baaki.exception.*;

import com.baaki.model.*;
import com.baaki.repository.InvoiceEventRepository;
import com.baaki.repository.InvoiceRepository;
import com.baaki.repository.LadderStageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Escalation ladder. For each open invoice the due stage is the highest enabled stage whose trigger date
 * (due date + offset) has passed and that is higher than any stage already sent. A stage is never sent twice.
 * Disputes, open inbox items, promises to pay and snoozes pause the ladder.
 */
@Service
public class LadderServiceImpl implements LadderService {
    public static final Set<String> STAGE_EVENT_TYPES = Set.of("REMINDER", "LETTER", "NEXT_STEPS");
    private final SnapshotService snapshots;
    private final InvoiceEventRepository events;
    private final InvoiceRepository invoices;
    private final LadderStageRepository stages;
    private final Clock clock;

    public LadderServiceImpl(SnapshotService snapshots, InvoiceEventRepository events, InvoiceRepository invoices,
                         LadderStageRepository stages, Clock clock) {
        this.snapshots = snapshots; this.events = events; this.invoices = invoices; this.stages = stages; this.clock = clock;
    }

    /** Pure decision used by the service and unit tests. Returns null when nothing is due. */
    public static LadderStage dueStage(List<LadderStage> ladder, LocalDate dueDate, LocalDate today,
                                       Set<String> sentStages, boolean paused) {
        if (paused) return null;
        int highestSent = -1;
        for (LadderStage st : ladder) if (sentStages.contains(st.code)) highestSent = Math.max(highestSent, st.sortOrder);
        LadderStage best = null;
        for (LadderStage st : ladder) {
            if (!st.enabled || st.sortOrder <= highestSent) continue;
            if (!dueDate.plusDays(st.offsetDays).isAfter(today)) {
                if (best == null || st.sortOrder > best.sortOrder) best = st;
            }
        }
        return best;
    }

    public static boolean isPaused(Snapshot s, Invoice i) {
        return "DISPUTED".equals(i.status) || s.invoicesWithOpenInbox.contains(i.id)
                || (i.promisedDate != null && !i.promisedDate.isBefore(s.today))
                || (i.snoozedUntil != null && !i.snoozedUntil.isBefore(s.today));
    }





    public Today today() {
        Snapshot s = snapshots.load();
        Map<Long, Set<String>> sent = sentStages(s);
        Map<String, FollowUp> groups = new LinkedHashMap<>();
        Map<String, LadderStage> byCode = new HashMap<>();
        s.ladder.forEach(l -> byCode.put(l.code, l));
        List<PausedItem> paused = new ArrayList<>();
        List<Invoice> sorted = s.invoices.stream().filter(s::isOpen).sorted(Comparator.comparing(i -> i.dueDate)).toList();
        for (Invoice i : sorted) {
            if (isPaused(s, i)) {
                String reason; LocalDate until = null;
                if ("DISPUTED".equals(i.status)) reason = "DISPUTE";
                else if (s.invoicesWithOpenInbox.contains(i.id)) reason = "INBOX";
                else if (i.promisedDate != null && !i.promisedDate.isBefore(s.today)) { reason = "PROMISE"; until = i.promisedDate; }
                else { reason = "SNOOZE"; until = i.snoozedUntil; }
                paused.add(new PausedItem(i.id, i.invoiceNumber, s.customer(i).name, reason, until));
                continue;
            }
            LadderStage st = dueStage(s.ladder, i.dueDate, s.today, sent.getOrDefault(i.id, Set.of()), false);
            if (st == null) continue;
            Customer c = s.customer(i);
            String key = c.id + ":" + st.code;
            FollowUp f = groups.computeIfAbsent(key, k -> new FollowUp(c.id, c.name, c.contactPerson, c.phone, c.email,
                    st.code, st.offsetDays, 0, 0, new ArrayList<>(), com.baaki.model.Languages.orEnglish(c.preferredLanguage)));
            f.invoices().add(new FollowUpInvoice(i.id, i.invoiceNumber, i.invoiceDate, i.dueDate, s.outstanding(i),
                    (int) java.time.temporal.ChronoUnit.DAYS.between(i.dueDate, s.today), i.payToken));
        }
        List<FollowUp> out = new ArrayList<>();
        for (FollowUp f : groups.values()) {
            long total = f.invoices().stream().mapToLong(FollowUpInvoice::outstandingPaise).sum();
            int max = f.invoices().stream().mapToInt(FollowUpInvoice::daysOverdue).max().orElse(0);
            out.add(new FollowUp(f.customerId(), f.customerName(), f.contactPerson(), f.phone(), f.email(), f.stage(),
                    f.stageOffset(), total, max, f.invoices(), f.language()));
        }
        out.sort(Comparator.comparingInt((FollowUp f) -> byCode.get(f.stage()).sortOrder).reversed()
                .thenComparing(FollowUp::totalPaise, Comparator.reverseOrder()));
        return new Today(s.today, out, paused);
    }

    private Map<Long, Set<String>> sentStages(Snapshot s) {
        Map<Long, Set<String>> m = new HashMap<>();
        s.eventsByInvoice.forEach((inv, list) -> list.forEach(e -> {
            if (STAGE_EVENT_TYPES.contains(e.type) && e.stage != null) m.computeIfAbsent(inv, k -> new HashSet<>()).add(e.stage);
        }));
        return m;
    }

    @Transactional
    public void markSent(List<Long> invoiceIds, String stage, String channel, String message) {
        LadderStage st = stages.findById(stage).orElseThrow(() -> new IllegalArgumentException("Unknown stage"));
        LocalDateTime now = LocalDateTime.now(clock);
        for (Long id : invoiceIds) {
            Invoice i = invoices.findById(id).orElseThrow(() -> new NoSuchElementException("Invoice not found"));
            boolean already = events.findByInvoiceIdOrderByEventAt(id).stream()
                    .anyMatch(e -> STAGE_EVENT_TYPES.contains(e.type) && st.code.equals(e.stage));
            if (already) throw new IllegalStateException("Stage " + stage + " was already sent for " + i.invoiceNumber);
            String type = switch (st.code) { case "DEMAND_LETTER" -> "LETTER"; case "NEXT_STEPS" -> "NEXT_STEPS"; default -> "REMINDER"; };
            events.save(new InvoiceEvent(id, i.customerId, now, type, st.code, channel, message));
        }
    }

    @Transactional
    public void snooze(Long invoiceId, LocalDate until) {
        Invoice i = invoices.findById(invoiceId).orElseThrow(() -> new NoSuchElementException("Invoice not found"));
        if (until == null || !until.isAfter(LocalDate.now(clock))) throw new IllegalArgumentException("Pick a future date");
        i.snoozedUntil = until;
        invoices.save(i);
        events.save(new InvoiceEvent(i.id, i.customerId, LocalDateTime.now(clock), "SNOOZE", null, null, "Reminders snoozed until " + until));
    }

    @Transactional
    public void promise(Long invoiceId, LocalDate date) {
        Invoice i = invoices.findById(invoiceId).orElseThrow(() -> new NoSuchElementException("Invoice not found"));
        if (date == null || date.isBefore(LocalDate.now(clock))) throw new IllegalArgumentException("Pick today or a future date");
        i.promisedDate = date;
        invoices.save(i);
        events.save(new InvoiceEvent(i.id, i.customerId, LocalDateTime.now(clock), "PROMISE", null, null, "Customer promised to pay on " + date));
    }
}
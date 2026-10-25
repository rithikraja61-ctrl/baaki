package com.baaki.service;

import com.baaki.dto.Snapshot;
import com.baaki.model.Invoice;
import com.baaki.model.LadderStage;
import java.time.LocalDate;
import java.util.*;

public interface LadderService {
    Set<String> STAGE_EVENT_TYPES = Set.of("REMINDER", "LETTER", "NEXT_STEPS");

    static LadderStage dueStage(List<LadderStage> ladder, LocalDate dueDate, LocalDate today,
                                Set<String> sentStages, boolean paused) {
        return com.baaki.service.impl.LadderServiceImpl.dueStage(ladder, dueDate, today, sentStages, paused);
    }

    static boolean isPaused(Snapshot s, Invoice i) {
        return com.baaki.service.impl.LadderServiceImpl.isPaused(s, i);
    }

    record FollowUpInvoice(Long invoiceId, String invoiceNumber, LocalDate invoiceDate, LocalDate dueDate,
                           long outstandingPaise, int daysOverdue, String payToken) {}

    record FollowUp(Long customerId, String customerName, String contactPerson, String phone, String email,
                    String stage, int stageOffset, long totalPaise, int maxDaysOverdue,
                    List<FollowUpInvoice> invoices, String language) {}

    record PausedItem(Long invoiceId, String invoiceNumber, String customerName, String reason, LocalDate until) {}

    record Today(LocalDate today, List<FollowUp> followUps, List<PausedItem> paused) {}

    Today today();
    void markSent(List<Long> invoiceIds, String stage, String channel, String message);
    void snooze(Long invoiceId, LocalDate until);
    void promise(Long invoiceId, LocalDate date);
}

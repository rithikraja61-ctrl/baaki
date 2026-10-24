package com.baaki.service;

import com.baaki.domain.Payment;
import com.baaki.repo.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class SnapshotService {
    private final Clock clock;
    private final BusinessProfileRepository profiles;
    private final CustomerRepository customers;
    private final InvoiceRepository invoices;
    private final PaymentAllocationRepository allocations;
    private final PaymentRepository payments;
    private final CreditNoteRepository creditNotes;
    private final InvoiceEventRepository events;
    private final InboxItemRepository inbox;
    private final ExpenseRepository expenses;
    private final PaymentHistoryRepository history;
    private final LadderStageRepository ladder;

    public SnapshotService(Clock clock, BusinessProfileRepository profiles, CustomerRepository customers,
                           InvoiceRepository invoices, PaymentAllocationRepository allocations, PaymentRepository payments,
                           CreditNoteRepository creditNotes, InvoiceEventRepository events, InboxItemRepository inbox,
                           ExpenseRepository expenses, PaymentHistoryRepository history, LadderStageRepository ladder) {
        this.clock = clock; this.profiles = profiles; this.customers = customers; this.invoices = invoices;
        this.allocations = allocations; this.payments = payments; this.creditNotes = creditNotes; this.events = events;
        this.inbox = inbox; this.expenses = expenses; this.history = history; this.ladder = ladder;
    }

    public LocalDate today() { return LocalDate.now(clock); }

    @Transactional(readOnly = true)
    public Snapshot load() {
        return new Snapshot(today(), profiles.findById(1L).orElseThrow(), customers.findAll(), invoices.findAll(),
                allocations.findAll(),
                payments.findAll().stream().collect(Collectors.toMap(p -> p.id, Function.<Payment>identity())),
                creditNotes.findAll(), events.findAll(), inbox.findByStatusOrderByCreatedAtDesc("OPEN"),
                expenses.findAll(), history.findAll(), ladder.findAllByOrderBySortOrder());
    }
}

package com.baaki.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "invoice_events")
public class InvoiceEvent {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    public Long invoiceId;
    public Long customerId;
    public LocalDateTime eventAt;
    /** CREATED, SENT, REMINDER, LETTER, PAYMENT, DISPUTE, PAID_CLAIM, PROMISE, SNOOZE, RESOLVED, CREDIT_NOTE, EDITED, NEXT_STEPS */
    public String type;
    public String stage;
    public String channel;
    @Column(columnDefinition = "TEXT") public String details;

    public InvoiceEvent() {}
    public InvoiceEvent(Long invoiceId, Long customerId, LocalDateTime at, String type, String stage, String channel, String details) {
        this.invoiceId = invoiceId; this.customerId = customerId; this.eventAt = at; this.type = type;
        this.stage = stage; this.channel = channel; this.details = details;
    }
}

package com.baaki.domain;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "inbox_items")
public class InboxItem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    public Long invoiceId;
    /** DISPUTE or PAID_CLAIM */
    public String kind;
    @Column(columnDefinition = "TEXT") public String reason;
    public String reference;
    public LocalDate paidDate;
    /** OPEN, ACCEPTED, REJECTED */
    public String status;
    @Column(columnDefinition = "TEXT") public String reply;
    public LocalDateTime createdAt;
    public LocalDateTime resolvedAt;
}

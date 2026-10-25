package com.baaki.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "send_log")
public class SendLog {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    public LocalDate sendDate;
    public LocalDateTime createdAt;
    public LocalDateTime updatedAt;
    public Long customerId;
    public String customerName;
    public String stage;
    /** Comma-separated invoice ids. */
    public String invoiceIds;
    public String invoiceNumbers;
    public String channel;
    public String recipient;
    public boolean testMode;
    /** AUTO (daily job), RUN_NOW (owner pressed the button), RETRY */
    public String origin;
    /** SENDING, SENT, FAILED, SKIPPED */
    public String status;
    public int attempts;
    public String providerId;
    @Column(columnDefinition = "TEXT") public String error;
}

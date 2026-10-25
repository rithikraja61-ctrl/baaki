package com.baaki.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "payments")
public class Payment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    public LocalDate paymentDate;
    public long amountPaise;
    public String reference;
    public String description;
    public String payerName;
    /** HISTORY, CSV, MANUAL, INBOX */
    public String source;
    /** PENDING (awaiting owner confirmation), CONFIRMED, IGNORED */
    public String status;
    public String note;
}

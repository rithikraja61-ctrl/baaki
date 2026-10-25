package com.baaki.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "credit_notes")
public class CreditNote {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    public String noteNumber;
    public Long invoiceId;
    public LocalDate noteDate;
    public long amountPaise;
    public String reason;
}

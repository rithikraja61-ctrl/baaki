package com.baaki.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "payment_history")
public class PaymentHistory {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    public Long customerId;
    public String reference;
    public LocalDate dueDate;
    public LocalDate paidDate;
    public long amountPaise;
}

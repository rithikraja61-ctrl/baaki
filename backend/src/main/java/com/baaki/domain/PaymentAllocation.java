package com.baaki.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "payment_allocations")
public class PaymentAllocation {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    public Long paymentId;
    public Long invoiceId;
    public long amountPaise;

    public PaymentAllocation() {}
    public PaymentAllocation(Long paymentId, Long invoiceId, long amountPaise) {
        this.paymentId = paymentId; this.invoiceId = invoiceId; this.amountPaise = amountPaise;
    }
}

package com.baaki.repository;

import com.baaki.model.PaymentAllocation;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentAllocationRepository extends JpaRepository<PaymentAllocation, Long> {
    List<PaymentAllocation> findByInvoiceId(Long invoiceId);
    List<PaymentAllocation> findByPaymentId(Long paymentId);
}

package com.baaki.repo;

import com.baaki.domain.PaymentAllocation;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentAllocationRepository extends JpaRepository<PaymentAllocation, Long> {
    List<PaymentAllocation> findByInvoiceId(Long invoiceId);
    List<PaymentAllocation> findByPaymentId(Long paymentId);
}

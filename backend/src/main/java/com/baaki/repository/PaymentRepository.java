package com.baaki.repository;

import com.baaki.model.Payment;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByStatusInOrderByPaymentDateDesc(List<String> statuses);
}

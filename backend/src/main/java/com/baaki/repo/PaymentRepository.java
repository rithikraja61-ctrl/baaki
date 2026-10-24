package com.baaki.repo;

import com.baaki.domain.Payment;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByStatusInOrderByPaymentDateDesc(List<String> statuses);
}

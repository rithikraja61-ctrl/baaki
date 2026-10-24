package com.baaki.repo;

import com.baaki.domain.PaymentHistory;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentHistoryRepository extends JpaRepository<PaymentHistory, Long> {
    List<PaymentHistory> findByCustomerId(Long customerId);
}

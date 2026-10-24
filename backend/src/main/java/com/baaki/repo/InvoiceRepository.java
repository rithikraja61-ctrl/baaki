package com.baaki.repo;

import com.baaki.domain.Invoice;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    Optional<Invoice> findByPayToken(String token);
    List<Invoice> findByCustomerId(Long customerId);
}

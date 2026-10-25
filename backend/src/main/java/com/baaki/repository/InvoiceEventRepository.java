package com.baaki.repository;

import com.baaki.model.InvoiceEvent;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvoiceEventRepository extends JpaRepository<InvoiceEvent, Long> {
    List<InvoiceEvent> findByInvoiceIdOrderByEventAt(Long invoiceId);
    List<InvoiceEvent> findByType(String type);
    List<InvoiceEvent> findByEventAtBetween(java.time.LocalDateTime from, java.time.LocalDateTime to);
}

package com.baaki.repo;

import com.baaki.domain.InvoiceEvent;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvoiceEventRepository extends JpaRepository<InvoiceEvent, Long> {
    List<InvoiceEvent> findByInvoiceIdOrderByEventAt(Long invoiceId);
    List<InvoiceEvent> findByType(String type);
}

package com.baaki.repo;

import com.baaki.domain.InvoiceLine;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvoiceLineRepository extends JpaRepository<InvoiceLine, Long> {
    List<InvoiceLine> findByInvoiceIdOrderByLineNo(Long invoiceId);
    void deleteByInvoiceId(Long invoiceId);
}

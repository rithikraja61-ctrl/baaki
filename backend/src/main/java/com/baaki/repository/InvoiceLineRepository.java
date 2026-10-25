package com.baaki.repository;

import com.baaki.model.InvoiceLine;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvoiceLineRepository extends JpaRepository<InvoiceLine, Long> {
    List<InvoiceLine> findByInvoiceIdOrderByLineNo(Long invoiceId);
    void deleteByInvoiceId(Long invoiceId);
}

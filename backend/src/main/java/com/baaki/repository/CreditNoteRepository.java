package com.baaki.repository;

import com.baaki.model.CreditNote;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CreditNoteRepository extends JpaRepository<CreditNote, Long> {
    List<CreditNote> findByInvoiceId(Long invoiceId);
}

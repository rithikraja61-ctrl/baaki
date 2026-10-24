package com.baaki.repo;

import com.baaki.domain.CreditNote;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CreditNoteRepository extends JpaRepository<CreditNote, Long> {
    List<CreditNote> findByInvoiceId(Long invoiceId);
}

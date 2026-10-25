package com.baaki.repository;

import com.baaki.model.InboxItem;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InboxItemRepository extends JpaRepository<InboxItem, Long> {
    List<InboxItem> findByStatusOrderByCreatedAtDesc(String status);
    List<InboxItem> findByInvoiceIdAndStatus(Long invoiceId, String status);
}

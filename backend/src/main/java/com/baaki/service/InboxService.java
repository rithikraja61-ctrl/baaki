package com.baaki.service;

import com.baaki.model.InboxItem;
import java.time.LocalDate;
import java.util.List;

public interface InboxService {
    record InboxView(InboxItem item, InvoiceService.InvoiceSummary invoice) {}
    record ClaimReq(String reference, LocalDate paidDate) {}
    record DisputeReq(String reason) {}
    record ResolveReq(String action, String reply, Long creditNotePaise) {}

    List<InboxView> list(String status);
    long openCount();
    void claimPaid(String token, ClaimReq r);
    void dispute(String token, DisputeReq r);
    void simulateUpiPayment(String token);
    void resolve(Long id, ResolveReq r);
}

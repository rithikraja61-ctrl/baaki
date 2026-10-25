package com.baaki.service;

import com.baaki.dto.Snapshot;
import com.baaki.model.Payment;
import com.baaki.model.PaymentAllocation;
import java.time.LocalDate;
import java.util.List;

public interface PaymentService {
    record PaymentView(Long id, LocalDate date, long amountPaise, String reference, String description,
                       String source, String status, String note, PaymentMatcher.Suggestion suggestion,
                       List<PaymentAllocation> allocations) {}

    record ImportResult(int imported, int skippedDuplicates, int debitsIgnored, List<String> errors) {}

    record ManualReq(LocalDate date, Long amountPaise, String reference, String description) {}
    record AllocReq(Long invoiceId, Long amountPaise) {}
    record ConfirmReq(List<AllocReq> allocations, String note) {}

    List<PaymentMatcher.OpenInvoice> openInvoices(Snapshot s);
    List<PaymentView> list();
    ImportResult importCsv(String csv);
    Payment addManual(ManualReq r);
    void confirm(Long paymentId, ConfirmReq req);
    void ignore(Long paymentId, String note);
}

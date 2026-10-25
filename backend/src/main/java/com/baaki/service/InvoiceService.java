package com.baaki.service;

import com.baaki.dto.Snapshot;
import com.baaki.model.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

public interface InvoiceService {
    record InvoiceSummary(Long id, String invoiceNumber, Long customerId, String customerName,
                          LocalDate invoiceDate, LocalDate dueDate, String status, long totalPaise,
                          long paidPaise, long creditedPaise, int daysOverdue, boolean paused,
                          String payToken) {}

    static InvoiceSummary summary(Snapshot s, Invoice i) {
        return com.baaki.service.impl.InvoiceServiceImpl.summary(s, i);
    }

    record Detail(InvoiceSummary summary, Invoice invoice, List<InvoiceLine> lines, Customer customer,
                  BusinessProfile profile, String qrPayload, boolean eInvoiceActive, String docTitle,
                  boolean composition, String amountInWords, List<InvoiceEvent> events,
                  List<Map<String, Object>> payments, List<CreditNote> creditNotes, List<InboxItem> inbox) {}

    record LineReq(String description, String hsnSac, BigDecimal quantity, String unit, Long ratePaise,
                   Long discountPaise, Integer gstRateBp) {}

    record InvoiceReq(Long customerId, LocalDate invoiceDate, LocalDate dueDate, String existingNumber,
                      String placeOfSupply, Boolean reverseCharge, String deliveryAddress, String notes,
                      List<LineReq> lines, Boolean send) {}

    static GstCalculator.DocType docTypeFor(String registrationType) {
        return com.baaki.service.impl.InvoiceServiceImpl.docTypeFor(registrationType);
    }

    List<InvoiceSummary> list();
    Detail detail(Long id);
    Detail detailByToken(String token);
    GstCalculator.Result preview(InvoiceReq r);
    Detail create(InvoiceReq r);
    Detail update(Long id, InvoiceReq r);
    Detail send(Long id);
    void deleteDraft(Long id);
    void refreshPaid(Long invoiceId);
}

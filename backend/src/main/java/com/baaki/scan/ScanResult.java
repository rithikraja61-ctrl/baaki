package com.baaki.scan;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** What the AI read, cleaned up and checked. Nothing here is saved: the owner reviews it in the New invoice form. */
public record ScanResult(
        String invoiceNumber, LocalDate invoiceDate, LocalDate printedDueDate, LocalDate dueDateFromCredit,
        String customerName, String customerGstin, Boolean customerGstinValid, String customerAddress,
        Long matchedCustomerId, String matchedBy, String placeOfSupply, String sellerGstin,
        List<Line> lines, Totals printed, Totals computed, List<String> uncertain, List<Flag> flags) {

    public record Line(String description, String hsnSac, BigDecimal quantity, String unit, Long ratePaise,
                       long discountPaise, int gstRateBp, Long printedTaxablePaise) {}

    public record Totals(Long taxablePaise, Long cgstPaise, Long sgstPaise, Long igstPaise, Long roundOffPaise, Long totalPaise) {}

    /** field = form field path (e.g. "customerGstin", "items.0.rate", "totals.total"); code is translated on the frontend. */
    public record Flag(String field, String code, Map<String, Object> params) {}
}

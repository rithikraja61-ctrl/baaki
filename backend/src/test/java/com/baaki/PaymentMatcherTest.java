package com.baaki;

import com.baaki.service.PaymentMatcher;
import com.baaki.service.PaymentMatcher.Kind;
import com.baaki.service.PaymentMatcher.OpenInvoice;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PaymentMatcherTest {
    // outstanding, taxable
    final OpenInvoice a = new OpenInvoice(1L, "INV/26-27/0001", 1L, "Kulkarni Pharma Distributors", 11_800_00, 10_000_00);
    final OpenInvoice b = new OpenInvoice(2L, "INV/26-27/0002", 2L, "Patil Agro Exports", 5_250_00, 5_000_00);
    final OpenInvoice c = new OpenInvoice(3L, "INV/26-27/0003", 2L, "Patil Agro Exports", 2_100_00, 2_000_00);
    final OpenInvoice d = new OpenInvoice(4L, "INV/26-27/0004", 3L, "Naik Cosmetics", 59_000_00, 50_000_00);
    final List<OpenInvoice> open = List.of(a, b, c, d);

    @Test
    void exactPayment() {
        var s = PaymentMatcher.match("NEFT CR KULKARNI PHARMA INV/26-27/0001", 11_800_00, open);
        assertEquals(Kind.EXACT, s.kind());
        assertEquals("HIGH", s.confidence());
        assertEquals(11_800_00, s.allocations().get(0).amountPaise());
    }

    @Test
    void partPayment() {
        var s = PaymentMatcher.match("IMPS KULKARNI PART INV/26-27/0001", 5_000_00, open);
        assertEquals(Kind.PART, s.kind());
        assertEquals(6_800_00, s.shortfallPaise());
        assertEquals(5_000_00, s.allocations().get(0).amountPaise());
    }

    @Test
    void onePaymentCoveringTwoInvoices() {
        var s = PaymentMatcher.match("RTGS PATIL AGRO INV/26-27/0002 INV/26-27/0003", 7_350_00, open);
        assertEquals(Kind.MULTIPLE, s.kind());
        assertEquals(2, s.allocations().size());
    }

    @Test
    void shortByTwoPercentOfTaxableIsTds() {
        var s = PaymentMatcher.match("NEFT NAIK COSMETICS INV/26-27/0004", 59_000_00 - 1_000_00, open);
        assertEquals(Kind.TDS, s.kind());
        assertEquals(2, s.tdsPercent());
        assertEquals(1_000_00, s.shortfallPaise());
    }

    @Test
    void oneAndTenPercentAreAlsoTds() {
        assertEquals(1, PaymentMatcher.tdsPercent(500_00, 50_000_00));
        assertEquals(10, PaymentMatcher.tdsPercent(5_000_00, 50_000_00));
        assertNull(PaymentMatcher.tdsPercent(1_234_00, 50_000_00));
    }

    @Test
    void overpayment() {
        var s = PaymentMatcher.match("NEFT KULKARNI INV/26-27/0001", 12_000_00, open);
        assertEquals(Kind.OVERPAID, s.kind());
        assertEquals(200_00, s.unallocatedPaise());
    }

    @Test
    void unknownDeposit() {
        var s = PaymentMatcher.match("CASH DEPOSIT CHIKHALI BRANCH", 15_000_00, open);
        assertEquals(Kind.UNMATCHED, s.kind());
        assertTrue(s.allocations().isEmpty());
    }

    @Test
    void nameAndAmountWithoutInvoiceNumber() {
        var s = PaymentMatcher.match("UPI NAIK COSMETICS", 59_000_00, open);
        assertEquals(Kind.EXACT, s.kind());
        assertEquals(4L, s.allocations().get(0).invoiceId());
    }
}

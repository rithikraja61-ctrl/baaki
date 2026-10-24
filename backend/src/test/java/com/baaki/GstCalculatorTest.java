package com.baaki;

import com.baaki.money.GstCalculator;
import com.baaki.money.GstCalculator.DocType;
import com.baaki.money.GstCalculator.LineInput;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GstCalculatorTest {
    static LineInput line(String qty, long ratePaise, long disc, int bp) {
        return new LineInput(new BigDecimal(qty), ratePaise, disc, bp);
    }

    @Test
    void tenThousandAt18PercentInsideMaharashtra() {
        var r = GstCalculator.compute(List.of(line("1", 10_000_00, 0, 1800)), "27", "27", DocType.TAX_INVOICE);
        assertEquals(1_000_000, r.taxablePaise());
        assertEquals(90_000, r.cgstPaise());
        assertEquals(90_000, r.sgstPaise());
        assertEquals(0, r.igstPaise());
        assertEquals(1_180_000, r.totalPaise());
        assertTrue(r.intraState());
    }

    @Test
    void tenThousandAt18PercentToGujaratIsIgst() {
        var r = GstCalculator.compute(List.of(line("1", 10_000_00, 0, 1800)), "27", "24", DocType.TAX_INVOICE);
        assertEquals(0, r.cgstPaise());
        assertEquals(180_000, r.igstPaise());
        assertEquals(1_180_000, r.totalPaise());
        assertFalse(r.intraState());
    }

    @Test
    void cgstAndSgstAreRoundedSeparatelyPerLine() {
        // taxable 3333 paise at 5%: half rate 2.5% = 83.325 -> 83 each
        var r = GstCalculator.compute(List.of(line("1", 3333, 0, 500)), "27", "27", DocType.TAX_INVOICE);
        assertEquals(83, r.cgstPaise());
        assertEquals(83, r.sgstPaise());
        // the same at IGST: 166.65 -> 167
        var i = GstCalculator.compute(List.of(line("1", 3333, 0, 500)), "27", "29", DocType.TAX_INVOICE);
        assertEquals(167, i.igstPaise());
    }

    @Test
    void discountReducesTaxableValueAndRoundOffIsSeparate() {
        // 3 x Rs 333.33 - Rs 10 = Rs 989.99, 18% intra: 89.0991 -> 89.10 each, total 1168.19 -> 1168, round off -0.19
        var r = GstCalculator.compute(List.of(line("3", 33333, 1000, 1800)), "27", "27", DocType.TAX_INVOICE);
        assertEquals(98999, r.taxablePaise());
        assertEquals(8910, r.cgstPaise());
        assertEquals(8910, r.sgstPaise());
        assertEquals(-19, r.roundOffPaise());
        assertEquals(116800, r.totalPaise());
    }

    @Test
    void roundOffGoesUpAtFiftyPaise() {
        var r = GstCalculator.compute(List.of(line("1", 10050, 0, 0)), "27", "27", DocType.TAX_INVOICE);
        assertEquals(50, r.roundOffPaise());
        assertEquals(10100, r.totalPaise());
    }

    @Test
    void multipleLinesWithDifferentRatesAddUp() {
        var r = GstCalculator.compute(List.of(line("1000", 1850, 0, 500), line("24", 5500, 0, 1800), line("1", 100000, 0, 4000), line("2", 50000, 0, 300)),
                "27", "27", DocType.TAX_INVOICE);
        assertEquals(1850000 + 132000 + 100000 + 100000, r.taxablePaise());
        assertEquals(r.cgstPaise(), r.sgstPaise());
        assertEquals(46250 + 11880 + 20000 + 1500, r.cgstPaise());
        assertEquals(r.taxablePaise() + r.cgstPaise() + r.sgstPaise() + r.roundOffPaise(), r.totalPaise());
        assertEquals(0, r.totalPaise() % 100);
    }

    @Test
    void billOfSupplyAndPlainInvoiceHaveNoGst() {
        var b = GstCalculator.compute(List.of(line("1", 10_000_00, 0, 1800)), "27", "27", DocType.BILL_OF_SUPPLY);
        assertEquals(0, b.cgstPaise() + b.sgstPaise() + b.igstPaise());
        assertEquals(1_000_000, b.totalPaise());
        var p = GstCalculator.compute(List.of(line("1", 10_000_00, 0, 1800)), "27", "24", DocType.INVOICE);
        assertEquals(0, p.igstPaise());
    }

    @Test
    void discountLargerThanLineIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> GstCalculator.compute(List.of(line("1", 100, 200, 1800)), "27", "27", DocType.TAX_INVOICE));
    }

    @Test
    void fractionalQuantity() {
        var r = GstCalculator.compute(List.of(line("2.5", 333, 0, 1800)), "27", "24", DocType.TAX_INVOICE);
        assertEquals(833, r.taxablePaise()); // 832.5 -> 833
        assertEquals(150, r.igstPaise());    // 149.94 -> 150
    }
}

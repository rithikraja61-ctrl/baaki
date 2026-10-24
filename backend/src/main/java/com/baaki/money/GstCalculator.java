package com.baaki.money;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * GST rules: tax per line on its taxable value (qty x rate - discount), rounded to the paisa.
 * Same state: CGST + SGST at half the rate each, rounded separately. Different state: IGST.
 * Grand total rounded to the nearest rupee with a separate round-off.
 */
public final class GstCalculator {
    private GstCalculator() {}

    public enum DocType { TAX_INVOICE, BILL_OF_SUPPLY, INVOICE }

    public record LineInput(BigDecimal quantity, long ratePaise, long discountPaise, int gstRateBp) {}

    public record LineResult(long taxablePaise, long cgstPaise, long sgstPaise, long igstPaise, long totalPaise) {}

    public record Result(List<LineResult> lines, long taxablePaise, long cgstPaise, long sgstPaise,
                         long igstPaise, long roundOffPaise, long totalPaise, boolean intraState) {}

    public static long taxableValue(BigDecimal qty, long ratePaise, long discountPaise) {
        long gross = qty.multiply(BigDecimal.valueOf(ratePaise)).setScale(0, RoundingMode.HALF_UP).longValueExact();
        return gross - discountPaise;
    }

    public static boolean isIntraState(String businessStateCode, String placeOfSupplyCode) {
        return businessStateCode != null && businessStateCode.equals(placeOfSupplyCode);
    }

    public static Result compute(List<LineInput> inputs, String businessState, String placeOfSupply, DocType docType) {
        boolean intra = isIntraState(businessState, placeOfSupply);
        boolean taxed = docType == DocType.TAX_INVOICE;
        List<LineResult> out = new ArrayList<>();
        long tx = 0, cg = 0, sg = 0, ig = 0;
        for (LineInput in : inputs) {
            if (in.discountPaise() < 0) throw new IllegalArgumentException("Discount cannot be negative");
            long taxable = taxableValue(in.quantity(), in.ratePaise(), in.discountPaise());
            if (taxable < 0) throw new IllegalArgumentException("Discount is larger than the line value");
            long c = 0, s = 0, i = 0;
            if (taxed) {
                if (intra) {
                    c = Money.mulDivRound(taxable, in.gstRateBp(), 20000); // half rate
                    s = Money.mulDivRound(taxable, in.gstRateBp(), 20000);
                } else {
                    i = Money.mulDivRound(taxable, in.gstRateBp(), 10000);
                }
            }
            out.add(new LineResult(taxable, c, s, i, taxable + c + s + i));
            tx += taxable; cg += c; sg += s; ig += i;
        }
        long gross = tx + cg + sg + ig;
        long rounded = Money.mulDivRound(gross, 1, 100) * 100;
        return new Result(out, tx, cg, sg, ig, rounded - gross, rounded, intra);
    }
}

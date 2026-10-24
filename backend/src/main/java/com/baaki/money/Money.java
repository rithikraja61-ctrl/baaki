package com.baaki.money;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Money helpers. All amounts are long paise. */
public final class Money {
    private Money() {}

    /** a * num / den rounded half-up (works for negatives too). */
    public static long mulDivRound(long a, long num, long den) {
        return BigDecimal.valueOf(a).multiply(BigDecimal.valueOf(num))
                .divide(BigDecimal.valueOf(den), 0, RoundingMode.HALF_UP).longValueExact();
    }

    /** Formats paise as ₹1,00,000.00 using Indian grouping. */
    public static String format(long paise) {
        boolean neg = paise < 0;
        long abs = Math.abs(paise);
        long rupees = abs / 100, p = abs % 100;
        String r = Long.toString(rupees);
        StringBuilder sb = new StringBuilder();
        if (r.length() > 3) {
            String head = r.substring(0, r.length() - 3);
            String tail = r.substring(r.length() - 3);
            StringBuilder h = new StringBuilder();
            for (int i = head.length(); i > 0; i -= 2) {
                h.insert(0, head.substring(Math.max(0, i - 2), i));
                if (i - 2 > 0) h.insert(0, ',');
            }
            sb.append(h).append(',').append(tail);
        } else sb.append(r);
        return (neg ? "-" : "") + "₹" + sb + "." + (p < 10 ? "0" : "") + p;
    }
}

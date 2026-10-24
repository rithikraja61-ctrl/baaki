package com.baaki.money;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * MSMED Act 2006, s.16: compound interest with monthly rests at three times the RBI bank rate,
 * from the appointed day (agreed date, at most 45 days from acceptance) until payment.
 * Whole months compound; a leftover part-month is charged pro rata on the compounded amount.
 */
public final class MsmeInterest {
    private MsmeInterest() {}

    public static long interestPaise(long principalPaise, BigDecimal bankRatePercent, LocalDate appointedDay, LocalDate asOf) {
        if (principalPaise <= 0 || !asOf.isAfter(appointedDay)) return 0;
        MathContext mc = MathContext.DECIMAL64;
        BigDecimal monthly = bankRatePercent.multiply(BigDecimal.valueOf(3)).divide(BigDecimal.valueOf(1200), mc);
        long months = ChronoUnit.MONTHS.between(appointedDay, asOf);
        LocalDate afterMonths = appointedDay.plusMonths(months);
        long days = ChronoUnit.DAYS.between(afterMonths, asOf);
        BigDecimal amt = BigDecimal.valueOf(principalPaise).multiply(BigDecimal.ONE.add(monthly).pow((int) months, mc), mc);
        amt = amt.multiply(BigDecimal.ONE.add(monthly.multiply(BigDecimal.valueOf(days)).divide(BigDecimal.valueOf(30), mc)), mc);
        return amt.setScale(0, RoundingMode.HALF_UP).longValueExact() - principalPaise;
    }
}

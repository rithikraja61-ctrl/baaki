package com.baaki.service;

import java.util.List;

/** Grades a customer A/B/C from how many days late each past payment was (negative = early). */
public final class ReliabilityCalculator {
    private ReliabilityCalculator() {}

    public record Stats(int payments, double avgDaysLate, int onTimePercent, String grade, String reasonCode) {}

    public static Stats compute(List<Integer> daysLate) {
        if (daysLate.isEmpty()) return new Stats(0, 0, 100, "B", "NO_HISTORY");
        double avg = daysLate.stream().mapToInt(d -> Math.max(0, d)).average().orElse(0);
        long onTime = daysLate.stream().filter(d -> d <= 0).count();
        int pct = (int) Math.round(onTime * 100.0 / daysLate.size());
        double avg1 = Math.round(avg * 10) / 10.0;
        String grade, reason;
        if (pct >= 80 && avg <= 3) { grade = "A"; reason = "MOSTLY_ON_TIME"; }
        else if (avg > 15 || pct < 30) { grade = "C"; reason = "USUALLY_VERY_LATE"; }
        else { grade = "B"; reason = "SOMETIMES_LATE"; }
        return new Stats(daysLate.size(), avg1, pct, grade, reason);
    }
}

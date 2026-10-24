package com.baaki.service;

import com.baaki.domain.Invoice;
import org.springframework.stereotype.Service;

import java.util.*;

/** "Chase these first": scores each overdue invoice. */
@Service
public class ChaseService {
    private final SnapshotService snapshots;
    private final ForecastService forecasts;

    public ChaseService(SnapshotService snapshots, ForecastService forecasts) {
        this.snapshots = snapshots; this.forecasts = forecasts;
    }

    public record ChaseItem(Long invoiceId, String invoiceNumber, Long customerId, String customerName,
                            long outstandingPaise, int daysOverdue, String grade, double avgDaysLate,
                            Integer coversShortfallWeek, boolean paused, String status, int score,
                            int amountPoints, int overduePoints, int reliabilityPoints, int gapPoints) {}

    public static int score(long outstanding, long maxOutstanding, int daysOverdue, String grade, boolean coversGap,
                            int[] parts) {
        int a = maxOutstanding <= 0 ? 0 : (int) Math.round(30.0 * Math.min(1.0, (double) outstanding / maxOutstanding));
        int o = (int) Math.round(30.0 * Math.min(1.0, daysOverdue / 60.0));
        int r = switch (grade) { case "C" -> 20; case "B" -> 12; default -> 5; };
        int g = coversGap ? 20 : 0;
        if (parts != null) { parts[0] = a; parts[1] = o; parts[2] = r; parts[3] = g; }
        return a + o + r + g;
    }

    public List<ChaseItem> list() {
        Snapshot s = snapshots.load();
        var rel = CustomerService.reliability(s);
        var fc = forecasts.forecast(s, ForecastService.WhatIf.none());
        Set<Long> cover = new HashSet<>();
        fc.coverGap().forEach(c -> cover.add(c.invoiceId()));
        List<Invoice> overdue = s.invoices.stream().filter(i -> s.isOpen(i) && s.daysOverdue(i) > 0).toList();
        long max = overdue.stream().mapToLong(s::outstanding).max().orElse(0);
        List<ChaseItem> out = new ArrayList<>();
        for (Invoice i : overdue) {
            var st = rel.get(i.customerId);
            int[] parts = new int[4];
            boolean c = cover.contains(i.id);
            int sc = score(s.outstanding(i), max, s.daysOverdue(i), st.grade(), c, parts);
            var sum = InvoiceService.summary(s, i);
            out.add(new ChaseItem(i.id, i.invoiceNumber, i.customerId, s.customer(i).name, s.outstanding(i),
                    s.daysOverdue(i), st.grade(), st.avgDaysLate(), c ? fc.firstShortfallWeek() : null, sum.paused(),
                    sum.status(), sc, parts[0], parts[1], parts[2], parts[3]));
        }
        out.sort(Comparator.comparingInt(ChaseItem::score).reversed().thenComparing(ChaseItem::outstandingPaise, Comparator.reverseOrder()));
        return out;
    }
}

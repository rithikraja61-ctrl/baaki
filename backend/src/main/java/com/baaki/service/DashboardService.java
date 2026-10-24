package com.baaki.service;

import com.baaki.domain.Invoice;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;

@Service
public class DashboardService {
    private final SnapshotService snapshots;
    private final ForecastService forecasts;
    private final ChaseService chase;
    private final InboxService inbox;

    public DashboardService(SnapshotService snapshots, ForecastService forecasts, ChaseService chase, InboxService inbox) {
        this.snapshots = snapshots; this.forecasts = forecasts; this.chase = chase; this.inbox = inbox;
    }

    public record Ageing(long notDue, long d1to30, long d31to60, long d61to90, long d90plus) {}

    public record Dashboard(LocalDate today, long toCollectPaise, long overduePaise, long expectedThisWeekPaise,
                            long expectedThisMonthPaise, long billsNext30Paise, long cashInBankPaise,
                            long lowestForecastPaise, Integer lowestWeek, Integer firstShortfallWeek,
                            long safetyBufferPaise, Ageing ageing, List<ChaseService.ChaseItem> chaseTop5,
                            long inboxOpen, int followUpsToday) {}

    public Dashboard get(int followUpsToday) {
        Snapshot s = snapshots.load();
        var rel = CustomerService.reliability(s);
        long toCollect = 0, overdue = 0, week = 0, month = 0;
        long[] age = new long[5];
        LocalDate weekEnd = s.today.with(DayOfWeek.SUNDAY);
        LocalDate monthEnd = s.today.withDayOfMonth(s.today.lengthOfMonth());
        for (Invoice i : s.invoices) {
            if (!s.isOpen(i)) continue;
            long o = s.outstanding(i);
            toCollect += o;
            int d = s.daysOverdue(i);
            if (d > 0) overdue += o;
            age[d == 0 ? 0 : d <= 30 ? 1 : d <= 60 ? 2 : d <= 90 ? 3 : 4] += o;
            if ("DISPUTED".equals(i.status)) continue;
            LocalDate exp = ForecastService.expectedDate(s, i, rel);
            if (!exp.isAfter(weekEnd)) week += o;
            if (!exp.isAfter(monthEnd)) month += o;
        }
        long bills = ForecastService.expenseFlows(s.expenses, s.today, s.today.plusDays(29), null).stream()
                .mapToLong(ForecastService.Flow::amountPaise).sum();
        var fc = forecasts.forecast(s, ForecastService.WhatIf.none());
        var top = chase.list();
        return new Dashboard(s.today, toCollect, overdue, week, month, bills, fc.cashInBankPaise(), fc.lowestPaise(),
                fc.lowestWeek(), fc.firstShortfallWeek(), fc.safetyBufferPaise(),
                new Ageing(age[0], age[1], age[2], age[3], age[4]), top.subList(0, Math.min(5, top.size())),
                inbox.openCount(), followUpsToday);
    }
}

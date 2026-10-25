package com.baaki.service;

import java.time.LocalDate;
import java.util.List;

public interface DashboardService {
    record Ageing(long notDue, long d1to30, long d31to60, long d61to90, long d90plus) {}

    record Dashboard(LocalDate today, long toCollectPaise, long overduePaise, long expectedThisWeekPaise,
                     long expectedThisMonthPaise, long billsNext30Paise, long cashInBankPaise,
                     long lowestForecastPaise, Integer lowestWeek, Integer firstShortfallWeek,
                     long safetyBufferPaise, Ageing ageing, List<ChaseService.ChaseItem> chaseTop5,
                     long inboxOpen, int followUpsToday) {}

    Dashboard get(int followUpsToday);
}

package com.baaki.service;

import com.baaki.dto.Snapshot;
import com.baaki.model.Expense;
import com.baaki.model.Invoice;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface ForecastService {
    record WhatIf(Long lateCustomerId, Integer lateDays, Long bigOrderPaise, LocalDate bigOrderDate,
                  Long delayedExpenseId, Integer expenseDelayDays) {
        public static WhatIf none() { return new WhatIf(null, null, null, null, null, null); }
    }

    record Flow(String kind, Long refId, String label, LocalDate date, long amountPaise) {}

    record Week(int week, LocalDate start, LocalDate end, long openingPaise, long inPaise, long outPaise,
                long closingPaise, boolean belowBuffer, List<Flow> ins, List<Flow> outs) {}

    record CoverItem(Long invoiceId, String invoiceNumber, String customerName, long outstandingPaise,
                     LocalDate expectedDate) {}

    record Forecast(LocalDate today, long cashInBankPaise, long safetyBufferPaise, List<Week> weeks,
                    Integer firstShortfallWeek, long shortfallPaise, long lowestPaise, Integer lowestWeek,
                    long totalInPaise, long totalOutPaise, List<CoverItem> coverGap,
                    long disputedExcludedPaise) {}

    long cashInBank(Snapshot s);

    static LocalDate expectedDate(Snapshot s, Invoice i, Map<Long, ReliabilityCalculator.Stats> rel) {
        return com.baaki.service.impl.ForecastServiceImpl.expectedDate(s, i, rel);
    }

    static List<Flow> expenseFlows(List<Expense> expenses, LocalDate from, LocalDate to, WhatIf w) {
        return com.baaki.service.impl.ForecastServiceImpl.expenseFlows(expenses, from, to, w);
    }

    Forecast forecast(WhatIf w);
    Forecast forecast(Snapshot s, WhatIf w);
}

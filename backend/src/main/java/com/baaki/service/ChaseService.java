package com.baaki.service;

import java.util.List;

public interface ChaseService {
    record ChaseItem(Long invoiceId, String invoiceNumber, Long customerId, String customerName,
                     long outstandingPaise, int daysOverdue, String grade, double avgDaysLate,
                     Integer coversShortfallWeek, boolean paused, String status, int score,
                     int amountPoints, int overduePoints, int reliabilityPoints, int gapPoints) {}

    static int score(long outstanding, long maxOutstanding, int daysOverdue, String grade, boolean coversGap,
                     int[] parts) {
        return com.baaki.service.impl.ChaseServiceImpl.score(outstanding, maxOutstanding, daysOverdue, grade, coversGap, parts);
    }

    List<ChaseItem> list();
}

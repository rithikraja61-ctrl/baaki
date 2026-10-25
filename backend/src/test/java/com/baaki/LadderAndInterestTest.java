package com.baaki;

import com.baaki.model.LadderStage;
import com.baaki.service.MsmeInterest;
import com.baaki.service.ChaseService;
import com.baaki.service.LadderService;
import com.baaki.service.ReliabilityCalculator;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class LadderAndInterestTest {
    static LadderStage st(String c, int off, int order) {
        LadderStage s = new LadderStage(); s.code = c; s.offsetDays = off; s.sortOrder = order; s.enabled = true; return s;
    }
    final List<LadderStage> ladder = List.of(st("HEADS_UP", -3, 1), st("DUE_TODAY", 0, 2), st("FIRM", 7, 3),
            st("FINAL", 15, 4), st("DEMAND_LETTER", 30, 5), st("NEXT_STEPS", 45, 6));
    final LocalDate due = LocalDate.of(2026, 10, 1);

    @Test
    void picksTheRightStage() {
        assertNull(LadderService.dueStage(ladder, due, due.minusDays(4), Set.of(), false));
        assertEquals("HEADS_UP", LadderService.dueStage(ladder, due, due.minusDays(3), Set.of(), false).code);
        assertEquals("DUE_TODAY", LadderService.dueStage(ladder, due, due, Set.of("HEADS_UP"), false).code);
        assertEquals("DEMAND_LETTER", LadderService.dueStage(ladder, due, due.plusDays(31), Set.of("FIRM"), false).code);
    }

    @Test
    void neverSendsTheSameStageTwice() {
        assertNull(LadderService.dueStage(ladder, due, due.plusDays(8), Set.of("FIRM"), false));
        assertNull(LadderService.dueStage(ladder, due, due.plusDays(20), Set.of("FINAL"), false));
    }

    @Test
    void pauseStopsEverything() {
        assertNull(LadderService.dueStage(ladder, due, due.plusDays(50), Set.of(), true));
    }

    @Test
    void msmeInterestIsCompoundMonthly() {
        // Rs 1,00,000 at 3 x 6% = 18% p.a. = 1.5% a month, 2 whole months: 100000 * 1.015^2 - 100000 = 3022.50
        long i = MsmeInterest.interestPaise(1_00_000_00, new BigDecimal("6"), LocalDate.of(2026, 1, 10), LocalDate.of(2026, 3, 10));
        assertEquals(3_022_50, i);
        assertEquals(0, MsmeInterest.interestPaise(1_00_000_00, new BigDecimal("6"), LocalDate.of(2026, 3, 10), LocalDate.of(2026, 3, 10)));
    }

    @Test
    void reliabilityGrades() {
        assertEquals("A", ReliabilityCalculator.compute(List.of(0, -1, 0, 2, 0)).grade());
        assertEquals("C", ReliabilityCalculator.compute(List.of(25, 30, 18, 40)).grade());
        assertEquals("B", ReliabilityCalculator.compute(List.of(5, 0, 8, 0)).grade());
    }

    @Test
    void chaseScoreGrowsWithAmountAgeAndRisk() {
        int low = ChaseService.score(10_000_00, 100_000_00, 5, "A", false, null);
        int high = ChaseService.score(100_000_00, 100_000_00, 60, "C", true, null);
        assertTrue(high > low);
        assertEquals(100, high);
    }
}

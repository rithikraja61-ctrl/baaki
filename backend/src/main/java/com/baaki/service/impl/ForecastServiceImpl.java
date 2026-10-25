package com.baaki.service.impl;

import com.baaki.service.ForecastService;
import com.baaki.service.*;
import com.baaki.model.*;
import com.baaki.repository.*;
import com.baaki.config.*;
import com.baaki.dto.*;
import com.baaki.exception.*;

import com.baaki.model.Expense;
import com.baaki.model.Invoice;
import com.baaki.model.Payment;
import com.baaki.repository.PaymentRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;

/** 13-week cash forecast. Closing cash of week n is the opening cash of week n+1. */
@Service
public class ForecastServiceImpl implements ForecastService {
    private final SnapshotService snapshots;
    private final PaymentRepository payments;

    public ForecastServiceImpl(SnapshotService snapshots, PaymentRepository payments) {
        this.snapshots = snapshots; this.payments = payments;
    }






    public long cashInBank(Snapshot s) {
        long cash = s.profile.cashInBankPaise;
        for (Payment p : payments.findAll())
            if ("CONFIRMED".equals(p.status) && !"HISTORY".equals(p.source)) cash += p.amountPaise;
        return cash;
    }

    /** Expected payment date: promised date, else due date + the customer's average delay (never before today). */
    public static LocalDate expectedDate(Snapshot s, Invoice i, Map<Long, ReliabilityCalculator.Stats> rel) {
        if (i.promisedDate != null && !i.promisedDate.isBefore(s.today)) return i.promisedDate;
        var st = rel.get(i.customerId);
        int delay = st == null ? 0 : (int) Math.round(st.avgDaysLate());
        LocalDate d = i.dueDate.plusDays(delay);
        return d.isBefore(s.today) ? s.today : d;
    }

    public static List<Flow> expenseFlows(List<Expense> expenses, LocalDate from, LocalDate to, WhatIf w) {
        List<Flow> out = new ArrayList<>();
        for (Expense e : expenses) {
            int delay = w != null && e.id != null && e.id.equals(w.delayedExpenseId()) && w.expenseDelayDays() != null ? w.expenseDelayDays() : 0;
            if ("MONTHLY".equals(e.recurrence) && e.dayOfMonth != null) {
                LocalDate m = from.withDayOfMonth(1).minusMonths(1);
                while (!m.isAfter(to)) {
                    LocalDate d = m.withDayOfMonth(Math.min(e.dayOfMonth, m.lengthOfMonth())).plusDays(delay);
                    if (!d.isBefore(from) && !d.isAfter(to)) out.add(new Flow("EXPENSE", e.id, e.name, d, e.amountPaise));
                    m = m.plusMonths(1);
                }
            } else if (e.dueDate != null) {
                LocalDate d = e.dueDate.plusDays(delay);
                if (d.isBefore(from) && !e.dueDate.isBefore(from)) d = from;
                if (!d.isBefore(from) && !d.isAfter(to)) out.add(new Flow("EXPENSE", e.id, e.name, d, e.amountPaise));
            }
        }
        out.sort(Comparator.comparing(Flow::date));
        return out;
    }

    public Forecast forecast(WhatIf w) { return forecast(snapshots.load(), w); }

    public Forecast forecast(Snapshot s, WhatIf w) {
        if (w == null) w = WhatIf.none();
        var rel = CustomerService.reliability(s);
        LocalDate from = s.today, to = s.today.plusDays(13 * 7 - 1);
        List<Flow> ins = new ArrayList<>();
        long disputed = 0;
        Map<Long, LocalDate> expected = new HashMap<>();
        for (Invoice i : s.invoices) {
            if (!s.isOpen(i)) continue;
            if ("DISPUTED".equals(i.status)) { disputed += s.outstanding(i); continue; }
            LocalDate d = expectedDate(s, i, rel);
            if (w.lateCustomerId() != null && w.lateCustomerId().equals(i.customerId))
                d = d.plusDays(w.lateDays() == null ? 30 : w.lateDays());
            expected.put(i.id, d);
            ins.add(new Flow("INVOICE", i.id, i.invoiceNumber + " · " + s.customer(i).name, d, s.outstanding(i)));
        }
        if (w.bigOrderPaise() != null && w.bigOrderPaise() > 0) {
            LocalDate d = w.bigOrderDate() == null ? s.today.plusDays(45) : w.bigOrderDate();
            ins.add(new Flow("WHAT_IF", null, "Big order (what if)", d.isBefore(from) ? from : d, w.bigOrderPaise()));
        }
        List<Flow> outs = expenseFlows(s.expenses, from, to, w);
        long cash = cashInBank(s);
        long buffer = s.profile.safetyBufferPaise;
        List<Week> weeks = new ArrayList<>();
        long open = cash, totalIn = 0, totalOut = 0, lowest = Long.MAX_VALUE;
        Integer firstShort = null, lowestWeek = null;
        long shortfall = 0;
        for (int k = 0; k < 13; k++) {
            LocalDate ws = from.plusDays(7L * k), we = ws.plusDays(6);
            List<Flow> wi = ins.stream().filter(f -> !f.date().isBefore(ws) && !f.date().isAfter(we)).toList();
            List<Flow> wo = outs.stream().filter(f -> !f.date().isBefore(ws) && !f.date().isAfter(we)).toList();
            long in = wi.stream().mapToLong(Flow::amountPaise).sum();
            long out = wo.stream().mapToLong(Flow::amountPaise).sum();
            long close = open + in - out;
            boolean below = close < buffer;
            if (below && firstShort == null) { firstShort = k + 1; shortfall = buffer - close; }
            if (close < lowest) { lowest = close; lowestWeek = k + 1; }
            weeks.add(new Week(k + 1, ws, we, open, in, out, close, below, wi, wo));
            totalIn += in; totalOut += out;
            open = close;
        }
        // Invoices that, if collected now, would cover the gap: open invoices expected after the shortfall week, largest first.
        List<CoverItem> cover = new ArrayList<>();
        if (firstShort != null) {
            LocalDate shortEnd = weeks.get(firstShort - 1).end();
            List<Invoice> cands = s.invoices.stream()
                    .filter(i -> expected.containsKey(i.id) && expected.get(i.id).isAfter(shortEnd))
                    .sorted(Comparator.comparingLong(s::outstanding).reversed()).toList();
            long need = shortfall;
            for (Invoice i : cands) {
                if (need <= 0) break;
                cover.add(new CoverItem(i.id, i.invoiceNumber, s.customer(i).name, s.outstanding(i), expected.get(i.id)));
                need -= s.outstanding(i);
            }
            if (need > 0) { // also consider overdue invoices already expected inside the window (chasing pulls them earlier)
                List<Invoice> overdueCands = s.invoices.stream().filter(x -> expected.containsKey(x.id) && s.daysOverdue(x) > 0
                        && cover.stream().noneMatch(c -> c.invoiceId().equals(x.id)))
                        .sorted(Comparator.comparingLong(s::outstanding).reversed()).toList();
                for (Invoice i : overdueCands) {
                    if (need <= 0) break;
                    cover.add(new CoverItem(i.id, i.invoiceNumber, s.customer(i).name, s.outstanding(i), expected.get(i.id)));
                    need -= s.outstanding(i);
                }
            }
        }
        return new Forecast(s.today, cash, buffer, weeks, firstShort, shortfall, lowest, lowestWeek, totalIn, totalOut, cover, disputed);
    }
}
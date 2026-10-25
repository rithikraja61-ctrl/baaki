package com.baaki.service.impl;

import com.baaki.service.LetterService;
import com.baaki.service.*;
import com.baaki.model.*;
import com.baaki.repository.*;
import com.baaki.config.*;
import com.baaki.dto.*;
import com.baaki.exception.*;

import com.baaki.config.BaakiProperties;
import com.baaki.model.*;
import com.baaki.service.IndianNumberWords;
import com.baaki.service.MsmeInterest;
import com.baaki.repository.InvoiceEventRepository;
import com.baaki.repository.InvoiceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/** Data for the "Payment Demand Letter": a plain request for payment from the business itself. */
@Service
public class LetterServiceImpl implements LetterService {
    public static final String TITLE = "Payment Demand Letter";
    private final SnapshotService snapshots;
    private final BaakiProperties props;
    private final InvoiceEventRepository events;
    private final InvoiceRepository invoices;
    private final Clock clock;

    public LetterServiceImpl(SnapshotService snapshots, BaakiProperties props, InvoiceEventRepository events,
                         InvoiceRepository invoices, Clock clock) {
        this.snapshots = snapshots; this.props = props; this.events = events; this.invoices = invoices; this.clock = clock;
    }

    public static String disclaimer(String businessName) {
        return "This letter is from " + businessName + " about money owed for the invoices listed below. "
                + "It is not a GST notice and it is not a notice from a lawyer.";
    }



    public Letter build(Long customerId, List<Long> invoiceIds, boolean includeInterest) {
        Snapshot s = snapshots.load();
        Customer c = Optional.ofNullable(s.customers.get(customerId)).orElseThrow(() -> new NoSuchElementException("Customer not found"));
        List<Invoice> invs = s.invoices.stream().filter(i -> i.customerId.equals(customerId) && s.isOpen(i)
                && (invoiceIds == null || invoiceIds.isEmpty() ? s.daysOverdue(i) > 0 : invoiceIds.contains(i.id)))
                .sorted(Comparator.comparing(i -> i.dueDate)).toList();
        if (invs.isEmpty()) throw new IllegalArgumentException("No unpaid invoices for this customer");
        boolean msme = s.isMicroOrSmall();
        BigDecimal rate = props.rbiBankRatePercent().multiply(BigDecimal.valueOf(3));
        List<LetterLine> lines = new ArrayList<>();
        long owed = 0, interest = 0;
        for (Invoice i : invs) {
            long o = s.outstanding(i);
            // appointed day: agreed due date, but never later than 45 days from the invoice date
            LocalDate appointed = i.dueDate.isAfter(i.invoiceDate.plusDays(props.msmeMaxDays())) ? i.invoiceDate.plusDays(props.msmeMaxDays()) : i.dueDate;
            long in = msme ? MsmeInterest.interestPaise(o, props.rbiBankRatePercent(), appointed, s.today) : 0;
            lines.add(new LetterLine(i.id, i.invoiceNumber, i.invoiceDate, i.dueDate, i.totalPaise, s.paid(i) + s.credited(i), o, s.daysOverdue(i), in));
            owed += o; interest += in;
        }
        boolean addInterest = msme && includeInterest;
        long demand = owed + (addInterest ? interest : 0);
        BusinessProfile b = s.profile;
        String msmeLine = msme ? "Under the MSMED Act, 2006, buyers should pay micro and small suppliers within the agreed period, which cannot exceed 45 days, and interest applies on late payment." : null;
        String upi = "upi://pay?pa=" + enc(b.upiId) + "&pn=" + enc(b.name) + "&am=" + (demand / 100) + "." + String.format("%02d", demand % 100)
                + "&tn=" + enc("Dues " + c.name) + "&cu=INR";
        return new Letter(TITLE, disclaimer(b.name), s.today, s.today.plusDays(props.letterDeadlineDays()), b, c, lines, owed,
                msme, msmeLine, props.rbiBankRatePercent(), rate, interest, addInterest, demand, IndianNumberWords.rupees(demand),
                upi, nextSteps(msme));
    }

    public static List<String> nextSteps(boolean msme) {
        List<String> l = new ArrayList<>(List.of("CALL_CUSTOMER", "CONSULT_LAWYER"));
        if (msme) l.add("MSME_SAMADHAAN");
        return l;
    }

    @Transactional
    public void logLetter(Long customerId, List<Long> invoiceIds, String channel) {
        LocalDateTime now = LocalDateTime.now(clock);
        for (Long id : invoiceIds) {
            Invoice i = invoices.findById(id).orElseThrow();
            if (!i.customerId.equals(customerId)) throw new IllegalArgumentException("Invoice belongs to another customer");
            boolean already = events.findByInvoiceIdOrderByEventAt(id).stream().anyMatch(e -> "DEMAND_LETTER".equals(e.stage));
            events.save(new InvoiceEvent(id, customerId, now, "LETTER", already ? null : "DEMAND_LETTER", channel,
                    TITLE + " issued" + (already ? " (copy)" : "")));
        }
    }

    private static String enc(String s) {
        return java.net.URLEncoder.encode(s == null ? "" : s, java.nio.charset.StandardCharsets.UTF_8).replace("+", "%20");
    }
}
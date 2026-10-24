package com.baaki.service;

import com.baaki.money.Money;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Suggests which invoices a bank credit pays, using the invoice number in the description, the amount and the
 * customer name. It only suggests: nothing is marked paid until the owner confirms.
 */
public final class PaymentMatcher {
    private PaymentMatcher() {}

    public static final Pattern INVOICE_NO = Pattern.compile("[A-Z]{2,5}/\\d{2}-\\d{2}/\\d{4}");

    public record OpenInvoice(Long id, String number, Long customerId, String customerName, long outstandingPaise,
                              long taxablePaise) {}

    public enum Kind { EXACT, MULTIPLE, PART, TDS, OVERPAID, UNMATCHED }

    public record Allocation(Long invoiceId, String invoiceNumber, long amountPaise) {}

    public record Suggestion(Kind kind, String confidence, List<Allocation> allocations, long unallocatedPaise,
                             Integer tdsPercent, long shortfallPaise, Long customerId, String customerName,
                             List<String> reasons) {}

    public static Suggestion match(String description, long amountPaise, List<OpenInvoice> open) {
        String desc = description == null ? "" : description.toUpperCase(Locale.ROOT);
        List<String> reasons = new ArrayList<>();
        List<OpenInvoice> refs = new ArrayList<>();
        Matcher m = INVOICE_NO.matcher(desc);
        while (m.find()) {
            String no = m.group();
            open.stream().filter(o -> no.equals(o.number())).findFirst().ifPresent(o -> { if (!refs.contains(o)) refs.add(o); });
        }
        OpenInvoice byName = null;
        Long nameCustomer = null; String nameCustomerName = null;
        for (OpenInvoice o : open) {
            if (nameMatches(desc, o.customerName())) { nameCustomer = o.customerId(); nameCustomerName = o.customerName(); break; }
        }
        if (!refs.isEmpty()) {
            reasons.add("Invoice number" + (refs.size() > 1 ? "s " : " ") + String.join(", ", refs.stream().map(OpenInvoice::number).toList()) + " in the description");
            Long cid = refs.get(0).customerId();
            String cname = refs.get(0).customerName();
            if (nameCustomer != null && nameCustomer.equals(cid)) reasons.add("Customer name matches");
            long sum = refs.stream().mapToLong(OpenInvoice::outstandingPaise).sum();
            String conf = nameCustomer != null && nameCustomer.equals(cid) ? "HIGH" : "MEDIUM";
            if (amountPaise == sum) {
                reasons.add("Amount " + Money.format(amountPaise) + " equals what is owed");
                List<Allocation> al = refs.stream().map(o -> new Allocation(o.id(), o.number(), o.outstandingPaise())).toList();
                return new Suggestion(refs.size() > 1 ? Kind.MULTIPLE : Kind.EXACT, conf, al, 0, null, 0, cid, cname, reasons);
            }
            if (amountPaise > sum) {
                long extra = amountPaise - sum;
                reasons.add("Paid " + Money.format(extra) + " more than owed");
                List<Allocation> al = refs.stream().map(o -> new Allocation(o.id(), o.number(), o.outstandingPaise())).toList();
                return new Suggestion(Kind.OVERPAID, conf, al, extra, null, 0, cid, cname, reasons);
            }
            long shortfall = sum - amountPaise;
            long taxable = refs.stream().mapToLong(OpenInvoice::taxablePaise).sum();
            Integer tds = tdsPercent(shortfall, taxable);
            List<Allocation> al = allocateOldestFirst(refs, amountPaise);
            if (tds != null) {
                reasons.add("Short by " + Money.format(shortfall) + ", exactly " + tds + "% of the taxable value");
                return new Suggestion(Kind.TDS, conf, al, 0, tds, shortfall, cid, cname, reasons);
            }
            reasons.add("Part payment: " + Money.format(shortfall) + " still owed");
            return new Suggestion(Kind.PART, conf, al, 0, null, shortfall, cid, cname, reasons);
        }
        // No invoice number: try customer name + amount.
        if (nameCustomer != null) {
            final Long cid = nameCustomer;
            List<OpenInvoice> theirs = open.stream().filter(o -> o.customerId().equals(cid)).toList();
            reasons.add("Customer name " + nameCustomerName + " in the description");
            for (OpenInvoice o : theirs) {
                if (o.outstandingPaise() == amountPaise) {
                    reasons.add("Amount equals " + o.number());
                    return new Suggestion(Kind.EXACT, "MEDIUM", List.of(new Allocation(o.id(), o.number(), amountPaise)), 0, null, 0, cid, nameCustomerName, reasons);
                }
            }
            long sum = theirs.stream().mapToLong(OpenInvoice::outstandingPaise).sum();
            if (amountPaise < sum) {
                reasons.add("No invoice number; applied to the oldest invoices first");
                return new Suggestion(Kind.PART, "LOW", allocateOldestFirst(theirs, amountPaise), 0, null, sum - amountPaise, cid, nameCustomerName, reasons);
            }
            reasons.add("Covers everything they owe");
            List<Allocation> al = theirs.stream().map(o -> new Allocation(o.id(), o.number(), o.outstandingPaise())).toList();
            return new Suggestion(amountPaise == sum ? Kind.MULTIPLE : Kind.OVERPAID, "LOW", al, amountPaise - sum, null, 0, cid, nameCustomerName, reasons);
        }
        List<OpenInvoice> sameAmount = open.stream().filter(o -> o.outstandingPaise() == amountPaise).toList();
        if (sameAmount.size() == 1) {
            OpenInvoice o = sameAmount.get(0);
            reasons.add("Only the amount matches " + o.number() + " (" + o.customerName() + ")");
            return new Suggestion(Kind.EXACT, "LOW", List.of(new Allocation(o.id(), o.number(), amountPaise)), 0, null, 0, o.customerId(), o.customerName(), reasons);
        }
        reasons.add("No invoice number, customer name or amount matches");
        return new Suggestion(Kind.UNMATCHED, "NONE", List.of(), amountPaise, null, 0, null, null, reasons);
    }

    /** Shortfall of exactly 1%, 2% or 10% of the taxable value (to the rupee, since banks round TDS). */
    public static Integer tdsPercent(long shortfallPaise, long taxablePaise) {
        if (shortfallPaise <= 0) return null;
        for (int pct : new int[]{1, 2, 10}) {
            long expect = Money.mulDivRound(taxablePaise, pct, 100);
            if (Math.abs(expect - shortfallPaise) < 100) return pct;
        }
        return null;
    }

    static List<Allocation> allocateOldestFirst(List<OpenInvoice> invs, long amount) {
        List<Allocation> out = new ArrayList<>();
        long left = amount;
        for (OpenInvoice o : invs) {
            if (left <= 0) break;
            long a = Math.min(left, o.outstandingPaise());
            out.add(new Allocation(o.id(), o.number(), a));
            left -= a;
        }
        return out;
    }

    static boolean nameMatches(String descUpper, String customerName) {
        if (customerName == null) return false;
        String[] words = customerName.toUpperCase(Locale.ROOT).replaceAll("[^A-Z ]", " ").trim().split("\\s+");
        // the first distinctive word (e.g. "KULKARNI") is enough; banks truncate names
        for (String w : words) {
            if (w.length() >= 4 && !Set.of("PRIVATE", "LIMITED", "PRODUCTS", "EXPORTS").contains(w))
                return descUpper.matches(".*\\b" + Pattern.quote(w) + "\\b.*");
        }
        return false;
    }
}

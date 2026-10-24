package com.baaki.autosend;

import com.baaki.domain.BusinessProfile;
import com.baaki.money.Money;
import com.baaki.service.LadderService.FollowUp;
import com.baaki.service.LadderService.FollowUpInvoice;

import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/** Builds the reminder email: stage message, invoice + pay links, and always ends with who to contact. */
public final class ReminderComposer {
    public record Email(String subject, String text, String html) {}

    private ReminderComposer() {}

    public static String fill(String tpl, Map<String, String> vars) {
        String s = tpl == null ? "" : tpl;
        for (Map.Entry<String, String> e : vars.entrySet()) s = s.replace("{" + e.getKey() + "}", e.getValue() == null ? "" : e.getValue());
        return s;
    }

    static String esc(String s) {
        return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    static boolean blank(String s) { return s == null || s.isBlank(); }

    public static String contactEmail(BusinessProfile p) { return blank(p.ownerEmail) ? p.email : p.ownerEmail; }

    /** @param testRealRecipient when not null, the email is a test copy and shows who it would have gone to. */
    public static Email compose(ReminderTemplates.Lang l, String lang, BusinessProfile p, FollowUp f, String baseUrl,
                                String testRealRecipient) {
        String base = blank(baseUrl) ? "" : (baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl);
        Locale loc = Locale.forLanguageTag(switch (lang == null ? "en" : lang) { case "hi" -> "hi-IN"; case "mr" -> "mr-IN"; default -> "en-IN"; });
        DateTimeFormatter df = DateTimeFormatter.ofPattern("d MMM yyyy", loc);
        String numbers = f.invoices().stream().map(FollowUpInvoice::invoiceNumber).collect(Collectors.joining(", "));
        String payLinks = f.invoices().stream().map(i -> base + "/pay/" + i.payToken()).collect(Collectors.joining(" "));
        Map<String, String> v = new LinkedHashMap<>();
        v.put("contact", blank(f.contactPerson()) ? f.customerName() : f.contactPerson());
        v.put("customer", f.customerName());
        v.put("business", p.name);
        v.put("invoices", numbers);
        v.put("amount", Money.format(f.totalPaise()));
        v.put("days", String.valueOf(Math.max(0, f.maxDaysOverdue())));
        v.put("dueDate", df.format(f.invoices().get(0).dueDate()));
        v.put("link", payLinks);
        v.put("owner", p.ownerName);
        v.put("phone", p.phone);
        v.put("email", contactEmail(p));

        String subject = fill(l.subject(), v);
        String body = fill(l.stages().get(f.stage()), v);
        String footer = fill(l.footer(), v);
        String banner = testRealRecipient == null ? null : fill(l.testBanner(), Map.of("to", testRealRecipient));

        StringBuilder t = new StringBuilder();
        StringBuilder h = new StringBuilder("<div style=\"font-family:Arial,sans-serif;font-size:16px;line-height:1.5;color:#111\">");
        if (banner != null) {
            t.append(banner).append("\n\n");
            h.append("<p style=\"background:#fff3cd;padding:8px;border-left:4px solid #b45309\">").append(esc(banner)).append("</p>");
        }
        t.append(body).append("\n\n").append(l.linksTitle()).append('\n');
        h.append("<p>").append(esc(body)).append("</p><p><strong>").append(esc(l.linksTitle())).append("</strong></p><ul>");
        for (FollowUpInvoice i : f.invoices()) {
            String inv = base + "/invoice/" + i.payToken(), pay = base + "/pay/" + i.payToken();
            t.append("- ").append(i.invoiceNumber()).append(" (").append(Money.format(i.outstandingPaise())).append(")\n")
             .append("  ").append(l.invoiceLink()).append(": ").append(inv).append('\n')
             .append("  ").append(l.payLink()).append(": ").append(pay).append('\n');
            h.append("<li>").append(esc(i.invoiceNumber())).append(" (").append(esc(Money.format(i.outstandingPaise()))).append(") — ")
             .append("<a href=\"").append(esc(inv)).append("\">").append(esc(l.invoiceLink())).append("</a> · ")
             .append("<a href=\"").append(esc(pay)).append("\">").append(esc(l.payLink())).append("</a></li>");
        }
        t.append('\n').append(footer);
        h.append("</ul><p style=\"border-top:1px solid #ccc;padding-top:8px\">").append(esc(footer)).append("</p></div>");
        return new Email(subject, t.toString(), h.toString());
    }
}

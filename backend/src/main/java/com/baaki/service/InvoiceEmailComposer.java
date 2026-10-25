package com.baaki.service;

import com.baaki.model.BusinessProfile;
import com.baaki.model.Customer;
import com.baaki.model.Invoice;
import com.baaki.model.InvoiceLine;

import java.time.format.DateTimeFormatter;
import java.util.List;

public final class InvoiceEmailComposer {
    public record Email(String subject, String text, String html, String recipient) {}

    private InvoiceEmailComposer() {}

    static String esc(String s) {
        return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    static boolean blank(String s) { return s == null || s.isBlank(); }

    public static Email compose(BusinessProfile p, Customer c, Invoice i, List<InvoiceLine> lines,
                                String baseUrl, boolean testMode) {
        String base = blank(baseUrl) ? "" : (baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl);
        DateTimeFormatter df = DateTimeFormatter.ofPattern("d MMM yyyy");
        String formattedDate = i.invoiceDate != null ? df.format(i.invoiceDate) : "";
        String formattedDue = i.dueDate != null ? df.format(i.dueDate) : "";
        String totalAmount = Money.format(i.totalPaise);
        String invLink = base + "/invoice/" + i.payToken;
        String payLink = base + "/pay/" + i.payToken;

        String recipient = testMode ? ReminderComposer.contactEmail(p) : c.email;
        if (blank(recipient)) recipient = ReminderComposer.contactEmail(p);

        String subject = (testMode ? "[TEST] " : "") + "Tax Invoice " + i.invoiceNumber + " from " + p.name + " (" + totalAmount + ")";

        // Plain text version
        StringBuilder t = new StringBuilder();
        if (testMode) {
            t.append("[TEST MODE] Sent to your test address. In live mode, this would go to ")
             .append(c.name).append(" (").append(c.email == null ? "no email" : c.email).append(").\n\n");
        }
        t.append("Dear ").append(blank(c.contactPerson) ? c.name : c.contactPerson).append(",\n\n")
         .append("Here is your invoice ").append(i.invoiceNumber).append(" for ").append(totalAmount).append(".\n")
         .append("Invoice Date: ").append(formattedDate).append("\n")
         .append("Due Date: ").append(formattedDue).append("\n\n")
         .append("View invoice: ").append(invLink).append("\n")
         .append("Pay online: ").append(payLink).append("\n\n")
         .append("Items:\n");
        if (lines != null) {
            for (InvoiceLine l : lines) {
                t.append("- ").append(l.description).append(" (Qty: ").append(l.quantity).append(" ").append(l.unit)
                 .append(") — ").append(Money.format(l.totalPaise)).append("\n");
            }
        }
        t.append("\nTaxable Value: ").append(Money.format(i.taxablePaise)).append("\n");
        if (i.cgstPaise > 0) t.append("CGST: ").append(Money.format(i.cgstPaise)).append("\n");
        if (i.sgstPaise > 0) t.append("SGST: ").append(Money.format(i.sgstPaise)).append("\n");
        if (i.igstPaise > 0) t.append("IGST: ").append(Money.format(i.igstPaise)).append("\n");
        t.append("Total: ").append(totalAmount).append("\n\n");
        if (!blank(p.bankName)) {
            t.append("Bank Details for NEFT/RTGS/IMPS:\n")
             .append("Bank: ").append(p.bankName).append("\n")
             .append("A/C No: ").append(p.accountNumber).append("\n")
             .append("IFSC: ").append(p.ifsc).append("\n");
            if (!blank(p.upiId)) t.append("UPI ID: ").append(p.upiId).append("\n");
            t.append("\n");
        }
        t.append("Regards,\n").append(p.ownerName == null ? p.name : p.ownerName).append("\n")
         .append(p.name).append("\nPhone: ").append(p.phone == null ? "" : p.phone);

        // HTML version
        StringBuilder h = new StringBuilder();
        h.append("<div style=\"font-family:Arial,sans-serif;font-size:15px;line-height:1.6;color:#222;max-width:640px;margin:0 auto;padding:16px;border:1px solid #e5e7eb;border-radius:8px;\">");
        if (testMode) {
            h.append("<div style=\"background:#fff3cd;padding:10px 14px;border-left:4px solid #b45309;margin-bottom:16px;font-size:13px;border-radius:4px;\">")
             .append("<strong>[TEST MODE]</strong> Sent to your test email. In live production, this would go to <strong>")
             .append(esc(c.name)).append("</strong> (").append(esc(c.email == null ? "no email" : c.email)).append(").")
             .append("</div>");
        }
        h.append("<h2 style=\"margin:0 0 4px 0;color:#065f46;\">").append(esc(p.name)).append("</h2>");
        if (!blank(p.gstin)) h.append("<p style=\"margin:0 0 12px 0;font-size:13px;color:#6b7280;\">GSTIN: ").append(esc(p.gstin)).append("</p>");
        h.append("<hr style=\"border:none;border-top:1px solid #e5e7eb;margin:12px 0;\" />");
        h.append("<p>Dear <strong>").append(esc(blank(c.contactPerson) ? c.name : c.contactPerson)).append("</strong>,</p>");
        h.append("<p>Please find attached the details of Tax Invoice <strong>").append(esc(i.invoiceNumber)).append("</strong> issued to you.</p>");

        h.append("<table style=\"width:100%;margin-bottom:16px;font-size:14px;background:#f9fafb;padding:12px;border-radius:6px;\">")
         .append("<tr><td><strong>Invoice Date:</strong> ").append(esc(formattedDate)).append("</td>")
         .append("<td style=\"text-align:right;\"><strong>Due Date:</strong> ").append(esc(formattedDue)).append("</td></tr>")
         .append("<tr><td><strong>Invoice No:</strong> ").append(esc(i.invoiceNumber)).append("</td>")
         .append("<td style=\"text-align:right;color:#065f46;font-size:18px;\"><strong>").append(esc(totalAmount)).append("</strong></td></tr>")
         .append("</table>");

        // Action buttons
        h.append("<div style=\"text-align:center;margin:24px 0;\">")
         .append("<a href=\"").append(esc(payLink)).append("\" style=\"background:#059669;color:#ffffff;padding:12px 24px;border-radius:6px;text-decoration:none;font-weight:bold;display:inline-block;margin-right:8px;\">Pay Online (UPI / Card)</a>")
         .append("<a href=\"").append(esc(invLink)).append("\" style=\"background:#374151;color:#ffffff;padding:12px 24px;border-radius:6px;text-decoration:none;font-weight:bold;display:inline-block;\">View & Print Invoice</a>")
         .append("</div>");

        // Line Items Table
        if (lines != null && !lines.isEmpty()) {
            h.append("<table style=\"width:100%;border-collapse:collapse;margin:16px 0;font-size:13px;\">")
             .append("<thead><tr style=\"background:#f3f4f6;text-align:left;\">")
             .append("<th style=\"padding:8px;border:1px solid #e5e7eb;\">Item</th>")
             .append("<th style=\"padding:8px;border:1px solid #e5e7eb;\">HSN</th>")
             .append("<th style=\"padding:8px;border:1px solid #e5e7eb;text-align:right;\">Qty</th>")
             .append("<th style=\"padding:8px;border:1px solid #e5e7eb;text-align:right;\">Rate</th>")
             .append("<th style=\"padding:8px;border:1px solid #e5e7eb;text-align:right;\">Total</th>")
             .append("</tr></thead><tbody>");
            for (InvoiceLine l : lines) {
                h.append("<tr>")
                 .append("<td style=\"padding:8px;border:1px solid #e5e7eb;\">").append(esc(l.description)).append("</td>")
                 .append("<td style=\"padding:8px;border:1px solid #e5e7eb;\">").append(esc(l.hsnSac)).append("</td>")
                 .append("<td style=\"padding:8px;border:1px solid #e5e7eb;text-align:right;\">").append(l.quantity).append(" ").append(esc(l.unit)).append("</td>")
                 .append("<td style=\"padding:8px;border:1px solid #e5e7eb;text-align:right;\">").append(Money.format(l.ratePaise)).append("</td>")
                 .append("<td style=\"padding:8px;border:1px solid #e5e7eb;text-align:right;font-weight:bold;\">").append(Money.format(l.totalPaise)).append("</td>")
                 .append("</tr>");
            }
            h.append("</tbody></table>");
        }

        // Totals
        h.append("<table style=\"width:100%;max-width:260px;margin-left:auto;font-size:13px;margin-bottom:20px;\">")
         .append("<tr><td>Taxable Value:</td><td style=\"text-align:right;\">").append(Money.format(i.taxablePaise)).append("</td></tr>");
        if (i.cgstPaise > 0) h.append("<tr><td>CGST:</td><td style=\"text-align:right;\">").append(Money.format(i.cgstPaise)).append("</td></tr>");
        if (i.sgstPaise > 0) h.append("<tr><td>SGST:</td><td style=\"text-align:right;\">").append(Money.format(i.sgstPaise)).append("</td></tr>");
        if (i.igstPaise > 0) h.append("<tr><td>IGST:</td><td style=\"text-align:right;\">").append(Money.format(i.igstPaise)).append("</td></tr>");
        h.append("<tr style=\"font-weight:bold;font-size:15px;border-top:1px solid #111;\"><td>Total Amount:</td><td style=\"text-align:right;\">").append(totalAmount).append("</td></tr>")
         .append("</table>");

        // Bank details
        if (!blank(p.bankName)) {
            h.append("<div style=\"background:#f3f4f6;padding:12px;border-radius:6px;font-size:13px;margin-top:16px;\">")
             .append("<strong>Bank Details for Direct Transfer:</strong><br />")
             .append("Bank: ").append(esc(p.bankName)).append(" · A/C No: <strong>").append(esc(p.accountNumber)).append("</strong> · IFSC: <strong>").append(esc(p.ifsc)).append("</strong>");
            if (!blank(p.upiId)) h.append("<br />UPI ID: <strong>").append(esc(p.upiId)).append("</strong>");
            h.append("</div>");
        }

        h.append("<p style=\"font-size:13px;color:#6b7280;margin-top:24px;border-top:1px solid #e5e7eb;padding-top:12px;\">")
         .append("Thank you for your business. For any questions, please contact ").append(esc(p.ownerName == null ? p.name : p.ownerName))
         .append(p.phone != null ? " at " + esc(p.phone) : "").append(".</p>")
         .append("</div>");

        return new Email(subject, t.toString(), h.toString(), recipient);
    }
}

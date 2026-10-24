package com.baaki.scan;

import com.baaki.config.BaakiProperties;
import com.baaki.domain.BusinessProfile;
import com.baaki.domain.Customer;
import com.baaki.domain.Invoice;
import com.baaki.money.GstCalculator;
import com.baaki.money.GstinValidator;
import com.baaki.money.IndianStates;
import com.baaki.repo.BusinessProfileRepository;
import com.baaki.repo.CustomerRepository;
import com.baaki.repo.InvoiceRepository;
import com.baaki.service.InvoiceService;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;

/**
 * Reads an existing paper/PDF invoice with AI, then checks it: GSTIN check digits, GST recalculated from the items
 * and compared with the printed totals, customer matched to Baaki's list. Never saves anything, never stores the file.
 */
@Service
public class InvoiceScanService {
    public static final long MAX_BYTES = 5L * 1024 * 1024;
    static final long TOLERANCE_PAISE = 100; // Rs 1, allows for rounding on the paper

    public record CustomerRef(Long id, String name, String gstin, String stateCode, int creditDays) {}

    public record Context(String businessState, String businessGstin, String registrationType, List<Integer> allowedRatesBp,
                          int hsnMinDigits, List<CustomerRef> customers, Set<String> existingNumbers) {}

    private final GeminiReader reader;
    private final AiProperties ai;
    private final BusinessProfileRepository profiles;
    private final CustomerRepository customers;
    private final InvoiceRepository invoices;
    private final BaakiProperties props;

    public InvoiceScanService(GeminiReader reader, AiProperties ai, BusinessProfileRepository profiles,
                              CustomerRepository customers, InvoiceRepository invoices, BaakiProperties props) {
        this.reader = reader; this.ai = ai; this.profiles = profiles; this.customers = customers; this.invoices = invoices; this.props = props;
    }

    public Map<String, Object> status() {
        return Map.of("configured", ai.configured(), "provider", "gemini", "model", ai.modelOrDefault(), "maxBytes", MAX_BYTES);
    }

    public ScanResult scan(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new ScanException("NO_FILE", 400, "Choose a photo or PDF first.");
        if (file.getSize() > MAX_BYTES) throw new ScanException("FILE_TOO_BIG", 413, "The file is larger than 5 MB.");
        byte[] data;
        try { data = file.getBytes(); } catch (IOException e) { throw new ScanException("NO_FILE", 400, "Could not read the file."); }
        try {
            String mime = sniff(data);
            if (mime == null) throw new ScanException("UNSUPPORTED_TYPE", 415, "Only JPG, PNG and PDF files can be read.");
            JsonNode raw = reader.read(data, mime);
            return build(raw, context());
        } finally {
            Arrays.fill(data, (byte) 0); // the file is only ever held in memory, and is wiped after reading
        }
    }

    Context context() {
        BusinessProfile p = profiles.findById(1L).orElseThrow();
        List<CustomerRef> cs = new ArrayList<>();
        for (Customer c : customers.findAll()) cs.add(new CustomerRef(c.id, c.name, c.gstin, c.stateCode, c.creditDays));
        Set<String> nums = new HashSet<>();
        for (Invoice i : invoices.findAll()) if (i.invoiceNumber != null) nums.add(i.invoiceNumber.toUpperCase());
        int hsnMin = p.turnoverPaise > props.hsnSixDigitThresholdPaise() ? 6 : 4;
        return new Context(p.stateCode, p.gstin, p.registrationType, props.gstRatesBp(), hsnMin, cs, nums);
    }

    /** Magic-byte check, so a renamed file cannot pretend to be an image. */
    static String sniff(byte[] d) {
        if (d.length >= 3 && (d[0] & 0xFF) == 0xFF && (d[1] & 0xFF) == 0xD8 && (d[2] & 0xFF) == 0xFF) return "image/jpeg";
        if (d.length >= 8 && (d[0] & 0xFF) == 0x89 && d[1] == 'P' && d[2] == 'N' && d[3] == 'G') return "image/png";
        if (d.length >= 5 && d[0] == '%' && d[1] == 'P' && d[2] == 'D' && d[3] == 'F' && d[4] == '-') return "application/pdf";
        return null;
    }

    // ------------------------------------------------------------------ pure logic (unit-tested)

    public static ScanResult build(JsonNode raw, Context ctx) {
        if (raw == null || !raw.path("isInvoice").asBoolean(true)) throw new ScanException("NOT_AN_INVOICE", 422, "This does not look like an invoice.");
        Set<String> unsure = new LinkedHashSet<>();
        for (JsonNode u : raw.path("uncertainFields")) if (u.isTextual()) unsure.add(u.asText().trim().replace("gstRatePercent", "gstRate"));
        List<ScanResult.Flag> flags = new ArrayList<>();

        String number = text(raw, "invoiceNumber");
        if (number == null) missing(flags, unsure, "invoiceNumber");
        else if (ctx.existingNumbers().contains(number.toUpperCase())) flags.add(flag("invoiceNumber", "ALREADY_IN_BAAKI", "number", number));

        LocalDate date = date(raw, "invoiceDate");
        if (date == null) missing(flags, unsure, "invoiceDate");
        LocalDate printedDue = date(raw, "dueDate");

        // GSTINs: check digit
        String custGstin = gstin(text(raw, "customerGstin"));
        Boolean custValid = null;
        if (custGstin != null) {
            custValid = GstinValidator.isValid(custGstin);
            if (!custValid) { flags.add(flag("customerGstin", "GSTIN_CHECK_FAILED", "gstin", custGstin)); unsure.add("customerGstin"); }
        }
        String sellerGstin = gstin(text(raw, "sellerGstin"));
        if (sellerGstin != null) {
            if (!GstinValidator.isValid(sellerGstin)) flags.add(flag("sellerGstin", "GSTIN_CHECK_FAILED", "gstin", sellerGstin));
            else if (ctx.businessGstin() != null && !ctx.businessGstin().equalsIgnoreCase(sellerGstin))
                flags.add(flag("sellerGstin", "NOT_YOUR_GSTIN", "gstin", sellerGstin));
        }

        // customer match
        String custName = text(raw, "customerName");
        if (custName == null) missing(flags, unsure, "customerName");
        CustomerRef match = null; String matchedBy = null;
        if (custGstin != null) for (CustomerRef c : ctx.customers()) if (custGstin.equalsIgnoreCase(c.gstin())) { match = c; matchedBy = "GSTIN"; break; }
        if (match == null && custName != null) {
            String n = norm(custName);
            for (CustomerRef c : ctx.customers()) {
                String m = norm(c.name());
                if (!m.isEmpty() && (m.equals(n) || (n.length() >= 6 && m.length() >= 6 && (m.startsWith(n) || n.startsWith(m))))) { match = c; matchedBy = "NAME"; break; }
            }
            if (match != null) { flags.add(flag("customer", "CUSTOMER_MATCHED_BY_NAME", "name", match.name())); unsure.add("customer"); }
        }
        if (match == null) flags.add(flag("customer", "CUSTOMER_NOT_FOUND", "name", custName == null ? "" : custName));

        // place of supply
        String pos = null;
        if (Boolean.TRUE.equals(custValid)) pos = GstinValidator.stateCode(custGstin);
        else if (match != null) pos = match.stateCode();
        else pos = stateFrom(text(raw, "customerState"));
        if (pos == null) { pos = ctx.businessState(); unsure.add("placeOfSupply"); }

        // printed totals
        ScanResult.Totals printed = new ScanResult.Totals(paise(raw.get("taxableTotal")), paise(raw.get("cgst")), paise(raw.get("sgst")),
                paise(raw.get("igst")), paise(raw.get("roundOff")), paise(raw.get("total")));
        if (printed.totalPaise() == null) missing(flags, unsure, "totals.total");
        Integer impliedBp = null;
        long printedTax = nz(printed.cgstPaise()) + nz(printed.sgstPaise()) + nz(printed.igstPaise());
        if (printed.taxablePaise() != null && printed.taxablePaise() > 0 && printedTax > 0)
            impliedBp = (int) Math.round(printedTax * 10000.0 / printed.taxablePaise());

        // lines
        List<ScanResult.Line> lines = new ArrayList<>();
        int i = 0;
        for (JsonNode it : raw.path("items")) {
            String p = "items." + i + ".";
            String desc = text(it, "description");
            if (desc == null) missing(flags, unsure, p + "description");
            String hsn = text(it, "hsnSac"); hsn = hsn == null ? "" : hsn.replaceAll("\\D", "");
            if (hsn.isEmpty()) missing(flags, unsure, p + "hsnSac");
            else if (hsn.length() < ctx.hsnMinDigits()) { flags.add(flag(p + "hsnSac", "HSN_TOO_SHORT", "digits", ctx.hsnMinDigits())); unsure.add(p + "hsnSac"); }
            BigDecimal qty = dec(it.get("quantity"));
            if (qty == null || qty.signum() <= 0) { qty = BigDecimal.ONE; missing(flags, unsure, p + "quantity"); }
            Long printedTaxable = paise(it.get("taxableValue"));
            Long rate = paise(it.get("rate"));
            if (rate == null && printedTaxable != null) {
                rate = BigDecimal.valueOf(printedTaxable).divide(qty, 0, RoundingMode.HALF_UP).longValue();
                unsure.add(p + "rate");
            } else if (rate == null) missing(flags, unsure, p + "rate");
            Long disc = paise(it.get("discount"));
            BigDecimal pct = dec(it.get("gstRatePercent"));
            int bp;
            if (pct != null) bp = pct.movePointRight(2).setScale(0, RoundingMode.HALF_UP).intValue();
            else { bp = impliedBp != null ? impliedBp : 1800; unsure.add(p + "gstRate"); }
            if (!ctx.allowedRatesBp().contains(bp)) {
                int printedBp = bp;
                bp = nearest(ctx.allowedRatesBp(), bp);
                flags.add(flag(p + "gstRate", "RATE_NOT_ALLOWED", "printed", printedBp, "used", bp));
                unsure.add(p + "gstRate");
            }
            lines.add(new ScanResult.Line(desc == null ? "" : desc, hsn, qty, text(it, "unit"), rate, disc == null ? 0 : Math.max(0, disc), bp, printedTaxable));
            i++;
        }
        if (lines.isEmpty()) flags.add(flag("items", "NO_ITEMS"));

        // recalculate GST from the items and compare with the printed totals
        ScanResult.Totals computed = null;
        if (!lines.isEmpty() && lines.stream().allMatch(l -> l.ratePaise() != null)) {
            try {
                List<GstCalculator.LineInput> in = new ArrayList<>();
                for (ScanResult.Line l : lines) in.add(new GstCalculator.LineInput(l.quantity(), l.ratePaise(), l.discountPaise(), l.gstRateBp()));
                GstCalculator.Result r = GstCalculator.compute(in, ctx.businessState(), pos, InvoiceService.docTypeFor(ctx.registrationType()));
                computed = new ScanResult.Totals(r.taxablePaise(), r.cgstPaise(), r.sgstPaise(), r.igstPaise(), r.roundOffPaise(), r.totalPaise());
                for (int k = 0; k < lines.size(); k++) {
                    Long pt = lines.get(k).printedTaxablePaise();
                    long ct = r.lines().get(k).taxablePaise();
                    if (pt != null && Math.abs(pt - ct) > TOLERANCE_PAISE)
                        flags.add(flag("items." + k + ".rate", "LINE_MISMATCH", "printed", pt, "computed", ct));
                }
                compare(flags, "totals.taxable", printed.taxablePaise(), r.taxablePaise());
                compare(flags, "totals.cgst", printed.cgstPaise(), r.cgstPaise());
                compare(flags, "totals.sgst", printed.sgstPaise(), r.sgstPaise());
                compare(flags, "totals.igst", printed.igstPaise(), r.igstPaise());
                compare(flags, "totals.total", printed.totalPaise(), r.totalPaise());
                boolean printedIntra = nz(printed.cgstPaise()) + nz(printed.sgstPaise()) > 0 && nz(printed.igstPaise()) == 0;
                boolean printedInter = nz(printed.igstPaise()) > 0 && nz(printed.cgstPaise()) + nz(printed.sgstPaise()) == 0;
                if ((printedIntra && !r.intraState() && r.igstPaise() > 0) || (printedInter && r.intraState() && r.cgstPaise() > 0)) {
                    flags.add(flag("placeOfSupply", "TAX_TYPE_DIFFERS", "printed", printedIntra ? "CGST_SGST" : "IGST"));
                    unsure.add("placeOfSupply");
                }
            } catch (IllegalArgumentException e) {
                flags.add(flag("items", "CANNOT_RECALCULATE"));
            }
        } else if (!lines.isEmpty()) flags.add(flag("items", "CANNOT_RECALCULATE"));

        LocalDate dueFromCredit = match != null && date != null ? date.plusDays(match.creditDays()) : null;
        if (printedDue != null && dueFromCredit != null && !printedDue.equals(dueFromCredit))
            flags.add(flag("dueDate", "DUE_DATE_DIFFERS", "printed", printedDue.toString(), "credit", dueFromCredit.toString()));

        return new ScanResult(number, date, printedDue, dueFromCredit, custName, custGstin, custValid, text(raw, "customerAddress"),
                match == null ? null : match.id(), matchedBy, pos, sellerGstin, lines, printed, computed, new ArrayList<>(unsure), flags);
    }

    private static void compare(List<ScanResult.Flag> flags, String field, Long printed, long computed) {
        if (printed != null && Math.abs(printed - computed) > TOLERANCE_PAISE) flags.add(flag(field, "TOTAL_MISMATCH", "printed", printed, "computed", computed));
    }

    private static void missing(List<ScanResult.Flag> flags, Set<String> unsure, String field) { flags.add(flag(field, "MISSING")); unsure.add(field); }

    static ScanResult.Flag flag(String field, String code, Object... kv) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int k = 0; k + 1 < kv.length; k += 2) m.put(String.valueOf(kv[k]), kv[k + 1]);
        return new ScanResult.Flag(field, code, m);
    }

    static int nearest(List<Integer> allowed, int bp) {
        int best = allowed.get(0);
        for (int a : allowed) if (Math.abs(a - bp) < Math.abs(best - bp)) best = a;
        return best;
    }

    static long nz(Long v) { return v == null ? 0 : v; }

    static String text(JsonNode n, String f) {
        JsonNode v = n == null ? null : n.get(f);
        if (v == null || v.isNull()) return null;
        String s = v.asText().trim();
        return s.isEmpty() || s.equalsIgnoreCase("null") ? null : s;
    }

    static String gstin(String s) { return s == null ? null : s.replaceAll("[\\s-]", "").toUpperCase(); }

    static String norm(String s) {
        return s == null ? "" : s.toLowerCase().replaceAll("m/s\\.?", "").replaceAll("\\b(pvt|private|ltd|limited|llp|co|company|and|the)\\b", "").replaceAll("[^a-z0-9]", "");
    }

    static String stateFrom(String s) {
        if (s == null) return null;
        String t = s.trim();
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\b(\\d{2})\\b").matcher(t);
        if (m.find() && IndianStates.BY_CODE.containsKey(m.group(1))) return m.group(1);
        String lower = t.toLowerCase();
        for (Map.Entry<String, String> e : IndianStates.BY_CODE.entrySet()) if (lower.contains(e.getValue().toLowerCase())) return e.getKey();
        return null;
    }

    static LocalDate date(JsonNode n, String f) {
        String s = text(n, f);
        if (s == null) return null;
        try { return LocalDate.parse(s); } catch (Exception e) { return null; }
    }

    static BigDecimal dec(JsonNode v) {
        if (v == null || v.isNull()) return null;
        if (v.isNumber()) return v.decimalValue();
        String s = v.asText().replaceAll("[^0-9.\\-]", "");
        if (s.isEmpty()) return null;
        try { return new BigDecimal(s); } catch (NumberFormatException e) { return null; }
    }

    static Long paise(JsonNode v) {
        BigDecimal d = dec(v);
        return d == null ? null : d.movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValue();
    }
}

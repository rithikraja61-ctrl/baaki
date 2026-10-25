package com.baaki.service;

import com.baaki.config.AiProperties;
import com.baaki.exception.ScanException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;

/**
 * Sends one invoice image or PDF to the Gemini API and returns the fields it read as JSON.
 * The file is sent inline (never uploaded to a file store) and nothing is written to disk.
 */
@Component
public class GeminiReader {
    static final String PROMPT = """
            You read Indian GST invoices (tax invoices, bills of supply). Extract exactly what is printed.
            Never guess or calculate a value that is not printed: use null instead.
            Amounts are in rupees as plain numbers, without commas or the rupee sign.
            Dates are YYYY-MM-DD. Indian invoices usually print dates as DD/MM/YYYY or DD-MM-YY.
            The customer is the buyer (Bill to, Buyer, Details of receiver, M/s), never the seller.
            rate is the price per unit before tax. taxableValue is the line value before tax.
            gstRatePercent is the total GST rate of the line: CGST% + SGST%, or IGST%.
            customerState is the state of the buyer or the place of supply, as printed.
            In uncertainFields, list every field you could not read clearly, had to interpret, or found smudged,
            using these names: invoiceNumber, invoiceDate, dueDate, customerName, customerGstin, customerState,
            sellerGstin, taxableTotal, cgst, sgst, igst, roundOff, total, and for items
            items.N.description, items.N.hsnSac, items.N.quantity, items.N.unit, items.N.rate, items.N.discount,
            items.N.gstRatePercent (N starts at 0).
            Set isInvoice to false if the picture is not an invoice.
            """;

    private final AiProperties props;
    private final ObjectMapper json = new ObjectMapper();
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    public GeminiReader(AiProperties props) { this.props = props; }

    public JsonNode read(byte[] data, String mimeType) {
        if (!props.configured()) throw new ScanException("AI_NOT_CONFIGURED", 503, "AI reading is not set up. Add AI_API_KEY to .env and restart the backend.");
        try {
            ObjectNode body = json.createObjectNode();
            ArrayNode parts = body.putArray("contents").addObject().put("role", "user").putArray("parts");
            parts.addObject().putObject("inline_data").put("mime_type", mimeType).put("data", Base64.getEncoder().encodeToString(data));
            parts.addObject().put("text", PROMPT);
            ObjectNode gen = body.putObject("generationConfig");
            gen.put("temperature", 0);
            gen.put("responseMimeType", "application/json");
            gen.set("responseSchema", schema());

            String model = URLEncoder.encode(props.modelOrDefault(), StandardCharsets.UTF_8);
            HttpRequest req = HttpRequest.newBuilder(URI.create("https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent"))
                    .timeout(Duration.ofSeconds(props.timeout()))
                    .header("x-goog-api-key", props.apiKey().trim())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body)))
                    .build();
            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
            int s = res.statusCode();
            if (s == 401 || s == 403) throw new ScanException("AI_KEY_REJECTED", 502, "The AI provider rejected the API key.");
            if (s == 429) throw new ScanException("AI_BUSY", 503, "The AI provider is busy or the free quota is used up. Try again in a minute.");
            if (s / 100 != 2) throw new ScanException("AI_FAILED", 502, "The AI provider returned an error (" + s + ").");
            JsonNode root = json.readTree(res.body());
            for (JsonNode p : root.path("candidates").path(0).path("content").path("parts")) {
                if (p.hasNonNull("text")) return json.readTree(p.get("text").asText());
            }
            throw new ScanException("AI_FAILED", 502, "The AI could not read this file.");
        } catch (ScanException e) {
            throw e;
        } catch (HttpTimeoutException e) {
            throw new ScanException("AI_TIMEOUT", 504, "The AI took too long. Try a clearer or smaller photo.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ScanException("AI_FAILED", 502, "Reading was interrupted.");
        } catch (Exception e) {
            throw new ScanException("AI_FAILED", 502, "The AI could not read this file.");
        }
    }

    private ObjectNode schema() {
        ObjectNode root = json.createObjectNode().put("type", "OBJECT");
        ObjectNode p = root.putObject("properties");
        for (String f : new String[]{"invoiceNumber", "invoiceDate", "dueDate", "sellerName", "sellerGstin",
                "customerName", "customerGstin", "customerAddress", "customerState"}) str(p, f);
        for (String f : new String[]{"taxableTotal", "cgst", "sgst", "igst", "roundOff", "total"}) num(p, f);
        ObjectNode item = p.putObject("items").put("type", "ARRAY").putObject("items").put("type", "OBJECT");
        ObjectNode ip = item.putObject("properties");
        for (String f : new String[]{"description", "hsnSac", "unit"}) str(ip, f);
        for (String f : new String[]{"quantity", "rate", "discount", "taxableValue", "gstRatePercent"}) num(ip, f);
        p.putObject("uncertainFields").put("type", "ARRAY").putObject("items").put("type", "STRING");
        p.putObject("isInvoice").put("type", "BOOLEAN");
        root.putArray("required").add("isInvoice").add("items").add("uncertainFields");
        return root;
    }

    private static void str(ObjectNode p, String f) { p.putObject(f).put("type", "STRING").put("nullable", true); }
    private static void num(ObjectNode p, String f) { p.putObject(f).put("type", "NUMBER").put("nullable", true); }
}

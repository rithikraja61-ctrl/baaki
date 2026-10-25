package com.baaki.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/** Loads reminder-templates.json (same file as frontend/src/data/reminder-templates.json). */
@Component
public class ReminderTemplates {
    public record Lang(String subject, String linksTitle, String invoiceLink, String payLink, String footer,
                       String testBanner, Map<String, String> stages) {}

    private final Map<String, Lang> byLang;

    public ReminderTemplates() {
        try (InputStream in = new ClassPathResource("reminder-templates.json").getInputStream()) {
            byLang = parse(new ObjectMapper().readTree(in));
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read reminder-templates.json", e);
        }
    }

    public static Map<String, Lang> parse(JsonNode root) {
        Map<String, Lang> m = new HashMap<>();
        for (String code : new String[]{"en", "hi", "mr"}) {
            JsonNode n = root.get(code);
            if (n == null) continue;
            Map<String, String> stages = new LinkedHashMap<>();
            n.get("stages").properties().forEach(e -> stages.put(e.getKey(), e.getValue().asText()));
            m.put(code, new Lang(n.get("subject").asText(), n.get("linksTitle").asText(), n.get("invoiceLink").asText(),
                    n.get("payLink").asText(), n.get("footer").asText(), n.get("testBanner").asText(), stages));
        }
        return m;
    }

    public Lang get(String lang) { return byLang.getOrDefault(lang, byLang.get("en")); }
}

package com.baaki.service.impl;

import com.baaki.service.EmailSender;
import com.baaki.service.*;
import com.baaki.model.*;
import com.baaki.repository.*;
import com.baaki.config.*;
import com.baaki.dto.*;
import com.baaki.exception.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Sends one email through Resend (HTTP API) or an SMTP account. The API key / password only come from env vars. */
@Component
public class EmailSenderImpl implements EmailSender {

    private final MailProperties props;
    private final ObjectProvider<JavaMailSender> smtp;
    private final ObjectMapper json = new ObjectMapper();
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    public EmailSenderImpl(MailProperties props, ObjectProvider<JavaMailSender> smtp) { this.props = props; this.smtp = smtp; }

    static boolean blank(String s) { return s == null || s.isBlank(); }

    public boolean configured() {
        if (blank(props.fromAddress())) return false;
        return switch (props.providerOrNone()) {
            case "resend" -> !blank(props.resendApiKey());
            case "smtp" -> smtp.getIfAvailable() != null;
            default -> false;
        };
    }

    /** Returns the provider's message id. Throws with a short reason on failure. */
    public String send(OutgoingEmail e) throws Exception {
        if (!configured()) throw new IllegalStateException("NOT_CONFIGURED");
        return "smtp".equals(props.providerOrNone()) ? sendSmtp(e) : sendResend(e);
    }

    private static String cleanName(String n) { return n == null ? "" : n.replaceAll("[\"<>\\r\\n]", "").trim(); }

    private String sendResend(OutgoingEmail e) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("from", cleanName(e.fromName()) + " <" + props.fromAddress().trim() + ">");
        body.put("to", List.of(e.to()));
        body.put("subject", e.subject());
        body.put("text", e.text());
        body.put("html", e.html());
        if (!blank(e.replyTo())) body.put("reply_to", e.replyTo());
        HttpRequest req = HttpRequest.newBuilder(URI.create("https://api.resend.com/emails"))
                .timeout(Duration.ofSeconds(20))
                .header("Authorization", "Bearer " + props.resendApiKey().trim())
                .header("Content-Type", "application/json")
                .header("Idempotency-Key", e.idempotencyKey())
                .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body)))
                .build();
        HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() / 100 != 2) {
            String b = res.body() == null ? "" : res.body();
            throw new IllegalStateException("Resend " + res.statusCode() + ": " + (b.length() > 300 ? b.substring(0, 300) : b));
        }
        JsonNode n = json.readTree(res.body());
        return n.hasNonNull("id") ? n.get("id").asText() : "resend";
    }

    private String sendSmtp(OutgoingEmail e) throws Exception {
        JavaMailSender s = smtp.getIfAvailable();
        if (s == null) throw new IllegalStateException("NOT_CONFIGURED");
        MimeMessage m = s.createMimeMessage();
        MimeMessageHelper h = new MimeMessageHelper(m, true, "UTF-8");
        h.setFrom(new InternetAddress(props.fromAddress().trim(), cleanName(e.fromName()), "UTF-8"));
        h.setTo(e.to());
        if (!blank(e.replyTo())) h.setReplyTo(e.replyTo());
        h.setSubject(e.subject());
        h.setText(e.text(), e.html());
        s.send(m);
        String id = m.getMessageID();
        return id == null ? "smtp" : id;
    }
}
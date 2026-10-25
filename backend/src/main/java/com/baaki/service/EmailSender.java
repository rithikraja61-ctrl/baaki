package com.baaki.service;

public interface EmailSender {
    record OutgoingEmail(String fromName, String to, String replyTo, String subject, String text, String html,
                         String idempotencyKey) {}

    boolean configured();
    String send(OutgoingEmail e) throws Exception;
}

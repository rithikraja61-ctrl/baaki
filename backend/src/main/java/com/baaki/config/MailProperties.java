package com.baaki.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Email settings from application.yml / environment variables. */
@ConfigurationProperties(prefix = "baaki.mail")
public record MailProperties(String provider, String fromAddress, String resendApiKey, String publicBaseUrl) {
    public String providerOrNone() { return provider == null || provider.isBlank() ? "none" : provider.trim().toLowerCase(); }
}

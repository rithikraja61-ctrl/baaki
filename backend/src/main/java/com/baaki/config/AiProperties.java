package com.baaki.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** AI invoice reading. The key only ever comes from the AI_API_KEY environment variable (see .env.example). */
@ConfigurationProperties(prefix = "baaki.ai")
public record AiProperties(String provider, String apiKey, String model, Integer timeoutSeconds) {
    public boolean configured() { return apiKey != null && !apiKey.isBlank(); }
    public String modelOrDefault() { return model == null || model.isBlank() ? "gemini-flash-latest" : model.trim(); }
    public int timeout() { return timeoutSeconds == null || timeoutSeconds <= 0 ? 60 : timeoutSeconds; }
}

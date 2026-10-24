package com.baaki.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;
import java.util.List;

/** Values from application.yml (the backend "config.js"). */
@ConfigurationProperties(prefix = "baaki")
public record BaakiProperties(
        List<Integer> gstRatesBp,
        BigDecimal rbiBankRatePercent,
        int letterDeadlineDays,
        int msmeMaxDays,
        long eInvoiceThresholdPaise,
        long hsnSixDigitThresholdPaise) {
}

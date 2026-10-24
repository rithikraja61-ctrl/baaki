package com.baaki.config;

import java.time.*;

/**
 * A Clock that honours the "Demo date" from Settings. When a demo date is set,
 * the date is replaced but the IST time of day keeps moving.
 */
public class DemoClock extends Clock {
    public static final ZoneId IST = ZoneId.of("Asia/Kolkata");
    private volatile LocalDate demoDate;

    public void setDemoDate(LocalDate d) { this.demoDate = d; }
    public LocalDate getDemoDate() { return demoDate; }

    @Override public ZoneId getZone() { return IST; }
    @Override public Clock withZone(ZoneId zone) { return this; }

    @Override
    public Instant instant() {
        LocalDate d = demoDate;
        if (d == null) return Instant.now();
        return d.atTime(LocalTime.now(IST)).atZone(IST).toInstant();
    }
}

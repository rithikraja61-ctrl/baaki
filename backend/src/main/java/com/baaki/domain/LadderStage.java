package com.baaki.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "ladder_stages")
public class LadderStage {
    @Id public String code;
    public int offsetDays;
    public int sortOrder;
    public boolean enabled;
    /** Send this stage by email automatically in the daily 10:00 IST job. Never allowed for DEMAND_LETTER / NEXT_STEPS. */
    public boolean autoSend;
}

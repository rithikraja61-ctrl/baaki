package com.baaki.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "ladder_stages")
public class LadderStage {
    @Id public String code;
    public int offsetDays;
    public int sortOrder;
    public boolean enabled;
}

package com.baaki.service;

import com.baaki.config.BaakiProperties;
import com.baaki.config.DemoClock;
import com.baaki.domain.BusinessProfile;
import com.baaki.domain.Expense;
import com.baaki.domain.LadderStage;
import com.baaki.money.GstinValidator;
import com.baaki.money.IndianStates;
import com.baaki.repo.BusinessProfileRepository;
import com.baaki.repo.ExpenseRepository;
import com.baaki.repo.LadderStageRepository;
import org.flywaydb.core.Flyway;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

@Service
public class SettingsService {
    private final BusinessProfileRepository profiles;
    private final LadderStageRepository ladder;
    private final ExpenseRepository expenses;
    private final DemoClock clock;
    private final Flyway flyway;
    private final BaakiProperties props;

    public SettingsService(BusinessProfileRepository profiles, LadderStageRepository ladder, ExpenseRepository expenses,
                           DemoClock clock, Flyway flyway, BaakiProperties props) {
        this.profiles = profiles; this.ladder = ladder; this.expenses = expenses; this.clock = clock;
        this.flyway = flyway; this.props = props;
    }

    public record Config(List<Integer> gstRatesBp, java.math.BigDecimal rbiBankRatePercent, int letterDeadlineDays,
                         int msmeMaxDays, long eInvoiceThresholdPaise, Map<String, String> states, LocalDate today) {}

    public Config config() {
        return new Config(props.gstRatesBp(), props.rbiBankRatePercent(), props.letterDeadlineDays(), props.msmeMaxDays(),
                props.eInvoiceThresholdPaise(), IndianStates.BY_CODE, LocalDate.now(clock));
    }

    public BusinessProfile profile() { return profiles.findById(1L).orElseThrow(); }

    @Transactional
    public BusinessProfile saveProfile(BusinessProfile in) {
        BusinessProfile p = profile();
        if (in.name == null || in.name.isBlank()) throw new IllegalArgumentException("Business name is required");
        if (!Set.of("REGULAR", "COMPOSITION", "UNREGISTERED").contains(in.registrationType))
            throw new IllegalArgumentException("Pick a registration type");
        if (!"UNREGISTERED".equals(in.registrationType)) {
            if (!GstinValidator.isValid(in.gstin)) throw new IllegalArgumentException("GSTIN is not valid");
            in.gstin = in.gstin.trim().toUpperCase();
            in.stateCode = GstinValidator.stateCode(in.gstin);
        } else {
            in.gstin = null;
            if (!IndianStates.BY_CODE.containsKey(in.stateCode)) throw new IllegalArgumentException("Pick a state");
        }
        if (in.turnoverPaise < 0 || in.safetyBufferPaise < 0) throw new IllegalArgumentException("Amounts cannot be negative");
        p.name = in.name.trim(); p.ownerName = in.ownerName; p.address = in.address; p.city = in.city;
        p.stateCode = in.stateCode; p.gstin = in.gstin; p.registrationType = in.registrationType;
        p.turnoverPaise = in.turnoverPaise; p.udyamRegistered = in.udyamRegistered; p.udyamNumber = in.udyamNumber;
        p.msmeCategory = in.msmeCategory; p.phone = in.phone; p.email = in.email; p.bankName = in.bankName;
        p.accountName = in.accountName; p.accountNumber = in.accountNumber; p.ifsc = in.ifsc; p.upiId = in.upiId;
        p.cashInBankPaise = in.cashInBankPaise; p.safetyBufferPaise = in.safetyBufferPaise;
        if (in.language != null) p.language = in.language;
        if (in.textSize != null) p.textSize = in.textSize;
        p.logoDataUrl = in.logoDataUrl;
        return profiles.save(p);
    }

    public record Prefs(String language, String textSize) {}

    @Transactional
    public BusinessProfile savePrefs(Prefs r) {
        BusinessProfile p = profile();
        if (r.language() != null) {
            if (!Set.of("en", "hi").contains(r.language())) throw new IllegalArgumentException("Unsupported language");
            p.language = r.language();
        }
        if (r.textSize() != null) p.textSize = r.textSize();
        return profiles.save(p);
    }

    @Transactional
    public LocalDate setDemoDate(LocalDate d) {
        BusinessProfile p = profile();
        p.demoDate = d;
        profiles.save(p);
        clock.setDemoDate(d);
        return LocalDate.now(clock);
    }

    public List<LadderStage> ladder() { return ladder.findAllByOrderBySortOrder(); }

    @Transactional
    public List<LadderStage> saveLadder(List<LadderStage> in) {
        Map<String, LadderStage> cur = new HashMap<>();
        ladder.findAll().forEach(l -> cur.put(l.code, l));
        int prev = Integer.MIN_VALUE;
        List<LadderStage> sorted = new ArrayList<>(in);
        sorted.sort(Comparator.comparingInt(l -> cur.containsKey(l.code) ? cur.get(l.code).sortOrder : 99));
        for (LadderStage l : sorted) {
            LadderStage c = cur.get(l.code);
            if (c == null) throw new IllegalArgumentException("Unknown stage " + l.code);
            if (l.offsetDays <= prev) throw new IllegalArgumentException("Each step must come after the one before it");
            prev = l.offsetDays;
            c.offsetDays = l.offsetDays; c.enabled = l.enabled;
            ladder.save(c);
        }
        return ladder();
    }

    public List<Expense> expenses() { return expenses.findAll(); }

    /** Restores schema and sample data from the Flyway migrations. */
    public synchronized void resetDemo() {
        flyway.clean();
        flyway.migrate();
        clock.setDemoDate(profile().demoDate);
    }
}

package com.baaki.service.impl;

import com.baaki.service.SettingsService;
import com.baaki.service.*;
import com.baaki.model.*;
import com.baaki.repository.*;
import com.baaki.config.*;
import com.baaki.dto.*;
import com.baaki.exception.*;

import com.baaki.config.BaakiProperties;
import com.baaki.config.DemoClock;
import com.baaki.model.BusinessProfile;
import com.baaki.model.Expense;
import com.baaki.model.LadderStage;
import com.baaki.service.GstinValidator;
import com.baaki.service.IndianStates;
import com.baaki.repository.BusinessProfileRepository;
import com.baaki.repository.ExpenseRepository;
import com.baaki.repository.LadderStageRepository;
import org.flywaydb.core.Flyway;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

@Service
public class SettingsServiceImpl implements SettingsService {
    private final BusinessProfileRepository profiles;
    private final LadderStageRepository ladder;
    private final ExpenseRepository expenses;
    private final DemoClock clock;
    private final Flyway flyway;
    private final BaakiProperties props;

    public SettingsServiceImpl(BusinessProfileRepository profiles, LadderStageRepository ladder, ExpenseRepository expenses,
                           DemoClock clock, Flyway flyway, BaakiProperties props) {
        this.profiles = profiles; this.ladder = ladder; this.expenses = expenses; this.clock = clock;
        this.flyway = flyway; this.props = props;
    }


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


    @Transactional
    public BusinessProfile savePrefs(Prefs r) {
        BusinessProfile p = profile();
        if (r.language() != null) {
            if (!com.baaki.model.Languages.ALL.contains(r.language())) throw new IllegalArgumentException("Unsupported language");
            p.language = r.language();
        }
        if (r.textSize() != null) p.textSize = r.textSize();
        if (r.docSecondLanguage() != null) {
            String d = r.docSecondLanguage().isBlank() || "none".equals(r.docSecondLanguage()) ? null : r.docSecondLanguage();
            if (d != null && !Set.of("hi", "mr").contains(d)) throw new IllegalArgumentException("Unsupported language");
            p.docSecondLanguage = d;
        }
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
            if (l.autoSend && com.baaki.service.AutoSendRules.NEVER_AUTO.contains(c.code))
                throw new IllegalArgumentException("This step is never sent automatically. You always press Send yourself.");
            c.offsetDays = l.offsetDays; c.enabled = l.enabled; c.autoSend = l.autoSend;
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
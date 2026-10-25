package com.baaki.service;

import com.baaki.model.BusinessProfile;
import com.baaki.model.Expense;
import com.baaki.model.LadderStage;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface SettingsService {
    record Config(List<Integer> gstRatesBp, java.math.BigDecimal rbiBankRatePercent, int letterDeadlineDays,
                  int msmeMaxDays, long eInvoiceThresholdPaise, Map<String, String> states, LocalDate today) {}

    record Prefs(String language, String textSize, String docSecondLanguage) {}

    Config config();
    BusinessProfile profile();
    BusinessProfile saveProfile(BusinessProfile in);
    BusinessProfile savePrefs(Prefs r);
    LocalDate setDemoDate(LocalDate d);
    List<LadderStage> ladder();
    List<LadderStage> saveLadder(List<LadderStage> in);
    List<Expense> expenses();
    void resetDemo();
}

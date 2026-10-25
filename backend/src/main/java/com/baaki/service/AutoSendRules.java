package com.baaki.service;

import com.baaki.model.LadderStage;
import com.baaki.service.LadderService.FollowUp;

import java.util.*;

/**
 * Pure decision: which of today's follow-ups the daily job may email. Paused, disputed and already-sent stages are
 * removed earlier by LadderService.today(). Here: only stages switched on for auto-send, never Payment Demand Letters
 * or next steps, at most one message per customer per day, and only when the customer has an email address.
 */
public final class AutoSendRules {
    public static final Set<String> NEVER_AUTO = Set.of("DEMAND_LETTER", "NEXT_STEPS");

    public record Decision(FollowUp followUp, String skip) {
        public boolean send() { return skip == null; }
    }

    private AutoSendRules() {}

    public static String key(Long customerId, String stage) { return customerId + ":" + stage; }

    /**
     * @param contactedToday customers who already got a message today (any channel, or an automatic attempt today)
     * @param alreadySent    customer:stage keys already sent (used in test mode, where the ladder is not advanced)
     */
    public static List<Decision> plan(List<FollowUp> followUps, Map<String, LadderStage> stages,
                                      Set<Long> contactedToday, Set<String> alreadySent) {
        List<FollowUp> sorted = new ArrayList<>(followUps);
        sorted.sort(Comparator.comparingInt((FollowUp f) -> stages.containsKey(f.stage()) ? stages.get(f.stage()).sortOrder : 0).reversed());
        Set<Long> chosen = new HashSet<>();
        List<Decision> out = new ArrayList<>();
        for (FollowUp f : sorted) {
            LadderStage st = stages.get(f.stage());
            String skip;
            if (NEVER_AUTO.contains(f.stage())) skip = "MANUAL_ONLY";
            else if (st == null || !st.enabled || !st.autoSend) skip = "AUTO_OFF";
            else if (alreadySent.contains(key(f.customerId(), f.stage()))) skip = "ALREADY_SENT";
            else if (contactedToday.contains(f.customerId())) skip = "CONTACTED_TODAY";
            else if (chosen.contains(f.customerId())) skip = "ONE_PER_DAY";
            else if (f.email() == null || f.email().isBlank()) skip = "NO_EMAIL";
            else skip = null;
            if (skip == null) chosen.add(f.customerId());
            out.add(new Decision(f, skip));
        }
        return out;
    }
}

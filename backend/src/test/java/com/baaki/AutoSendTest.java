package com.baaki;

import com.baaki.autosend.AutoSendRules;
import com.baaki.autosend.ReminderComposer;
import com.baaki.autosend.ReminderTemplates;
import com.baaki.domain.BusinessProfile;
import com.baaki.domain.LadderStage;
import com.baaki.service.LadderService.FollowUp;
import com.baaki.service.LadderService.FollowUpInvoice;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class AutoSendTest {
    static LadderStage st(String c, int order, boolean auto) {
        LadderStage s = new LadderStage(); s.code = c; s.sortOrder = order; s.enabled = true; s.autoSend = auto; return s;
    }
    static final Map<String, LadderStage> STAGES = Map.of(
            "HEADS_UP", st("HEADS_UP", 1, true), "DUE_TODAY", st("DUE_TODAY", 2, true), "FIRM", st("FIRM", 3, false),
            "FINAL", st("FINAL", 4, false), "DEMAND_LETTER", st("DEMAND_LETTER", 5, true), "NEXT_STEPS", st("NEXT_STEPS", 6, true));

    static FollowUp fu(long customer, String stage, String email) {
        FollowUpInvoice i = new FollowUpInvoice(customer * 10, "INV/26-27/00" + customer, LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 10, 24), 1180000, 0, "tok" + customer);
        return new FollowUp(customer, "Customer " + customer, "Contact", "+91 90000 00000", email, stage, 0, 1180000, 0, List.of(i));
    }

    static String skip(List<AutoSendRules.Decision> plan, long customer, String stage) {
        return plan.stream().filter(d -> d.followUp().customerId() == customer && d.followUp().stage().equals(stage))
                .findFirst().orElseThrow().skip();
    }

    @Test
    void onlySwitchedOnStagesAreSent() {
        var plan = AutoSendRules.plan(List.of(fu(1, "HEADS_UP", "a@x.in"), fu(2, "FIRM", "b@x.in")), STAGES, Set.of(), Set.of());
        assertNull(skip(plan, 1, "HEADS_UP"));
        assertEquals("AUTO_OFF", skip(plan, 2, "FIRM"));
    }

    @Test
    void demandLettersAreNeverAutomatic() {
        var plan = AutoSendRules.plan(List.of(fu(1, "DEMAND_LETTER", "a@x.in"), fu(2, "NEXT_STEPS", "b@x.in")), STAGES, Set.of(), Set.of());
        assertEquals("MANUAL_ONLY", skip(plan, 1, "DEMAND_LETTER"));
        assertEquals("MANUAL_ONLY", skip(plan, 2, "NEXT_STEPS"));
    }

    @Test
    void oneMessagePerCustomerPerDay() {
        var plan = AutoSendRules.plan(List.of(fu(1, "HEADS_UP", "a@x.in"), fu(1, "DUE_TODAY", "a@x.in")), STAGES, Set.of(), Set.of());
        assertNull(skip(plan, 1, "DUE_TODAY"));          // the higher stage wins
        assertEquals("ONE_PER_DAY", skip(plan, 1, "HEADS_UP"));
        var plan2 = AutoSendRules.plan(List.of(fu(3, "HEADS_UP", "c@x.in")), STAGES, Set.of(3L), Set.of());
        assertEquals("CONTACTED_TODAY", skip(plan2, 3, "HEADS_UP"));
    }

    @Test
    void neverTheSameStageTwiceAndNeedsEmail() {
        var plan = AutoSendRules.plan(List.of(fu(1, "HEADS_UP", "a@x.in"), fu(2, "HEADS_UP", " ")), STAGES, Set.of(), Set.of("1:HEADS_UP"));
        assertEquals("ALREADY_SENT", skip(plan, 1, "HEADS_UP"));
        assertEquals("NO_EMAIL", skip(plan, 2, "HEADS_UP"));
    }

    @Test
    void emailHasLinksAndEndsWithWhoToContact() {
        BusinessProfile p = new BusinessProfile();
        p.name = "Sharma Packaging"; p.ownerName = "Rakesh Sharma"; p.phone = "+91 98900 12345"; p.ownerEmail = "rakesh@sp.example";
        var l = new ReminderTemplates().get("en");
        var e = ReminderComposer.compose(l, "en", p, fu(1, "DUE_TODAY", "a@x.in"), "https://baaki.example/", null);
        assertTrue(e.subject().startsWith("Sharma Packaging"));
        assertTrue(e.text().contains("https://baaki.example/pay/tok1"));
        assertTrue(e.text().contains("https://baaki.example/invoice/tok1"));
        assertTrue(e.text().trim().endsWith("Questions? Contact Rakesh Sharma, Sharma Packaging: +91 98900 12345, rakesh@sp.example."));
        assertFalse(e.text().toLowerCase().contains("legal notice"));
        var test = ReminderComposer.compose(l, "en", p, fu(1, "DUE_TODAY", "a@x.in"), "https://baaki.example", "a@x.in");
        assertTrue(test.text().startsWith("TEST MODE"));
    }
}

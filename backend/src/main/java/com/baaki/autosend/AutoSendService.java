package com.baaki.autosend;

import com.baaki.domain.BusinessProfile;
import com.baaki.domain.InvoiceEvent;
import com.baaki.domain.LadderStage;
import com.baaki.repo.BusinessProfileRepository;
import com.baaki.repo.InvoiceEventRepository;
import com.baaki.repo.LadderStageRepository;
import com.baaki.service.LadderService;
import com.baaki.service.LadderService.FollowUp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Daily job (10:00 IST) that emails the reminders due today for the stages the owner switched on.
 * Every attempt is written to send_log before the provider is called, so a crash can never cause a silent resend.
 */
@Service
public class AutoSendService {
    private static final Logger log = LoggerFactory.getLogger(AutoSendService.class);

    private final LadderService ladder;
    private final LadderStageRepository stages;
    private final BusinessProfileRepository profiles;
    private final InvoiceEventRepository events;
    private final SendLogRepository sendLog;
    private final EmailSender sender;
    private final ReminderTemplates templates;
    private final MailProperties mail;
    private final Clock clock;

    public AutoSendService(LadderService ladder, LadderStageRepository stages, BusinessProfileRepository profiles,
                           InvoiceEventRepository events, SendLogRepository sendLog, EmailSender sender,
                           ReminderTemplates templates, MailProperties mail, Clock clock) {
        this.ladder = ladder; this.stages = stages; this.profiles = profiles; this.events = events; this.sendLog = sendLog;
        this.sender = sender; this.templates = templates; this.mail = mail; this.clock = clock;
    }

    public record RunResult(LocalDate date, boolean testMode, int sent, int failed, List<Map<String, String>> skipped) {}

    @Scheduled(cron = "${baaki.auto-send.cron:0 0 10 * * *}", zone = "Asia/Kolkata")
    public void daily() {
        BusinessProfile p = profiles.findById(1L).orElse(null);
        if (p == null || !p.autoSendEnabled) { log.info("Automatic reminders are switched off"); return; }
        RunResult r = run("AUTO");
        log.info("Automatic reminders {}: sent={} failed={} skipped={} testMode={}", r.date(), r.sent(), r.failed(), r.skipped().size(), r.testMode());
    }

    public synchronized RunResult run(String origin) {
        BusinessProfile p = profiles.findById(1L).orElseThrow();
        LocalDate today = LocalDate.now(clock);
        Map<String, LadderStage> byCode = stages.findAll().stream().collect(Collectors.toMap(s -> s.code, s -> s));
        List<FollowUp> due = ladder.today().followUps();
        Set<String> alreadySent = p.autoSendTestMode
                ? sendLog.findByTestModeTrueAndStatus("SENT").stream().map(l -> AutoSendRules.key(l.customerId, l.stage)).collect(Collectors.toSet())
                : Set.of();
        List<AutoSendRules.Decision> plan = AutoSendRules.plan(due, byCode, contactedToday(today, null), alreadySent);
        int sent = 0, failed = 0;
        List<Map<String, String>> skipped = new ArrayList<>();
        for (AutoSendRules.Decision d : plan) {
            if (!d.send()) {
                skipped.add(Map.of("customer", d.followUp().customerName(), "stage", d.followUp().stage(), "reason", d.skip()));
                continue;
            }
            supersedeOldFailures(d.followUp());
            SendLog row = newRow(d.followUp(), p, today, origin);
            if (attempt(row, d.followUp(), p)) sent++; else failed++;
        }
        return new RunResult(today, p.autoSendTestMode, sent, failed, skipped);
    }

    /** Retry a failed send. Re-checks every rule against today's data first. */
    public synchronized SendLog retry(Long id) {
        SendLog row = sendLog.findById(id).orElseThrow(() -> new NoSuchElementException("Send not found"));
        if (!"FAILED".equals(row.status)) throw new IllegalStateException("Only failed sends can be retried");
        BusinessProfile p = profiles.findById(1L).orElseThrow();
        LocalDate today = LocalDate.now(clock);
        FollowUp f = ladder.today().followUps().stream()
                .filter(x -> x.customerId().equals(row.customerId) && x.stage().equals(row.stage)).findFirst().orElse(null);
        if (f == null) {
            close(row, "SKIPPED", "This reminder is no longer due (paid, paused, disputed or already sent).");
            throw new IllegalStateException("This reminder is no longer due, so it was not sent.");
        }
        if (AutoSendRules.NEVER_AUTO.contains(f.stage())) throw new IllegalStateException("Payment Demand Letters are only sent by the owner.");
        if (contactedToday(today, row.id).contains(f.customerId()))
            throw new IllegalStateException("This customer already got a message today. Try again tomorrow.");
        if (f.email() == null || f.email().isBlank()) throw new IllegalStateException("This customer has no email address.");
        row.sendDate = today;
        row.origin = "RETRY";
        row.testMode = p.autoSendTestMode;
        row.invoiceIds = f.invoices().stream().map(i -> String.valueOf(i.invoiceId())).collect(Collectors.joining(","));
        row.invoiceNumbers = f.invoices().stream().map(LadderService.FollowUpInvoice::invoiceNumber).collect(Collectors.joining(", "));
        row.recipient = p.autoSendTestMode ? ReminderComposer.contactEmail(p) : f.email();
        row.status = "SENDING";
        row.updatedAt = LocalDateTime.now(clock);
        sendLog.save(row);
        attempt(row, f, p);
        return row;
    }

    private SendLog newRow(FollowUp f, BusinessProfile p, LocalDate today, String origin) {
        SendLog r = new SendLog();
        r.sendDate = today;
        r.createdAt = r.updatedAt = LocalDateTime.now(clock);
        r.customerId = f.customerId();
        r.customerName = f.customerName();
        r.stage = f.stage();
        r.invoiceIds = f.invoices().stream().map(i -> String.valueOf(i.invoiceId())).collect(Collectors.joining(","));
        r.invoiceNumbers = f.invoices().stream().map(LadderService.FollowUpInvoice::invoiceNumber).collect(Collectors.joining(", "));
        r.channel = "EMAIL";
        r.testMode = p.autoSendTestMode;
        r.recipient = p.autoSendTestMode ? ReminderComposer.contactEmail(p) : f.email();
        r.origin = origin;
        r.status = "SENDING";
        return sendLog.save(r);
    }

    private boolean attempt(SendLog row, FollowUp f, BusinessProfile p) {
        row.attempts++;
        try {
            if (row.recipient == null || row.recipient.isBlank()) throw new IllegalStateException("No owner email set for test mode");
            String lang = com.baaki.domain.Languages.orEnglish(f.language()); // customer's preferred language
            ReminderComposer.Email e = ReminderComposer.compose(templates.get(lang), lang, p, f, mail.publicBaseUrl(),
                    p.autoSendTestMode ? f.email() : null);
            String subject = p.autoSendTestMode ? "[TEST] " + e.subject() : e.subject();
            row.providerId = sender.send(new EmailSender.OutgoingEmail(p.name, row.recipient, ReminderComposer.contactEmail(p),
                    subject, e.text(), e.html(), "baaki-" + row.id + "-" + row.attempts));
            close(row, "SENT", null);
            if (!p.autoSendTestMode) {
                // Real send: record it on each invoice timeline so the ladder moves on and the stage is never sent again.
                try {
                    List<Long> ids = f.invoices().stream().map(LadderService.FollowUpInvoice::invoiceId).toList();
                    ladder.markSent(ids, f.stage(), "EMAIL", "Automatic email to " + row.recipient + ": " + e.subject());
                } catch (Exception markEx) {
                    log.warn("Email sent but timeline not updated for log {}: {}", row.id, markEx.getMessage());
                }
            }
            return true;
        } catch (Exception ex) {
            String msg = ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
            close(row, "FAILED", msg.length() > 1000 ? msg.substring(0, 1000) : msg);
            log.warn("Reminder email to customer {} ({}) failed: {}", row.customerId, row.stage, msg);
            return false;
        }
    }

    private void close(SendLog row, String status, String error) {
        row.status = status;
        row.error = error;
        row.updatedAt = LocalDateTime.now(clock);
        sendLog.save(row);
    }

    private void supersedeOldFailures(FollowUp f) {
        for (SendLog old : sendLog.findTop100ByOrderByIdDesc()) {
            if ("FAILED".equals(old.status) && old.customerId.equals(f.customerId()) && old.stage.equals(f.stage()))
                close(old, "SKIPPED", "Replaced by a newer attempt");
        }
    }

    /** Customers who already got (or are getting) a message today: any manual or automatic send. */
    Set<Long> contactedToday(LocalDate today, Long ignoreLogId) {
        Set<Long> s = new HashSet<>();
        for (InvoiceEvent e : events.findByEventAtBetween(today.atStartOfDay(), today.plusDays(1).atStartOfDay()))
            if (LadderService.STAGE_EVENT_TYPES.contains(e.type)) s.add(e.customerId);
        for (SendLog l : sendLog.findBySendDate(today))
            if (!l.id.equals(ignoreLogId) && Set.of("SENT", "SENDING").contains(l.status)) s.add(l.customerId);
        return s;
    }

    public record Status(boolean configured, String provider, boolean enabled, boolean testMode, String ownerEmail,
                         String fromAddress, List<SendLog> failed, List<SendLog> today) {}

    public Status status() {
        BusinessProfile p = profiles.findById(1L).orElseThrow();
        LocalDate today = LocalDate.now(clock);
        List<SendLog> recent = sendLog.findTop100ByOrderByIdDesc();
        return new Status(sender.configured(), mail.providerOrNone(), p.autoSendEnabled, p.autoSendTestMode, p.ownerEmail,
                mail.fromAddress(), recent.stream().filter(l -> "FAILED".equals(l.status)).toList(),
                recent.stream().filter(l -> today.equals(l.sendDate)).toList());
    }

    public List<SendLog> recent() { return sendLog.findTop100ByOrderByIdDesc(); }

    public record SettingsReq(Boolean enabled, Boolean testMode, String ownerEmail) {}

    public Status saveSettings(SettingsReq r) {
        BusinessProfile p = profiles.findById(1L).orElseThrow();
        if (r.ownerEmail() != null) {
            String e = r.ownerEmail().trim();
            if (!e.isEmpty() && !e.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) throw new IllegalArgumentException("Enter a valid email address");
            p.ownerEmail = e.isEmpty() ? null : e;
        }
        if (r.enabled() != null) p.autoSendEnabled = r.enabled();
        if (r.testMode() != null) p.autoSendTestMode = r.testMode();
        if (p.autoSendTestMode && (p.ownerEmail == null || p.ownerEmail.isBlank()) && (p.email == null || p.email.isBlank()))
            throw new IllegalArgumentException("Test mode needs your own email address");
        profiles.save(p);
        return status();
    }
}

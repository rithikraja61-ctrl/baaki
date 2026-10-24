package com.baaki.web;

import com.baaki.domain.BusinessProfile;
import com.baaki.domain.LadderStage;
import com.baaki.money.GstCalculator;
import com.baaki.money.GstinValidator;
import com.baaki.money.IndianNumberWords;
import com.baaki.money.IndianStates;
import com.baaki.service.*;
import com.baaki.autosend.AutoSendService;
import com.baaki.autosend.SendLog;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api")
public class ApiController {
    private final DashboardService dashboard;
    private final InvoiceService invoices;
    private final CustomerService customers;
    private final LadderService ladder;
    private final ChaseService chase;
    private final ForecastService forecast;
    private final PaymentService payments;
    private final InboxService inbox;
    private final LetterService letters;
    private final SettingsService settings;
    private final AutoSendService autoSend;

    public ApiController(DashboardService dashboard, InvoiceService invoices, CustomerService customers,
                         LadderService ladder, ChaseService chase, ForecastService forecast, PaymentService payments,
                         InboxService inbox, LetterService letters, SettingsService settings,
                         AutoSendService autoSend) {
        this.dashboard = dashboard; this.invoices = invoices; this.customers = customers; this.ladder = ladder;
        this.chase = chase; this.forecast = forecast; this.payments = payments; this.inbox = inbox;
        this.letters = letters; this.settings = settings; this.autoSend = autoSend;
    }

    // ----- config & settings -----
    @GetMapping("/config") public SettingsService.Config config() { return settings.config(); }
    @GetMapping("/settings/profile") public BusinessProfile profile() { return settings.profile(); }
    @PutMapping("/settings/profile") public BusinessProfile saveProfile(@RequestBody BusinessProfile p) { return settings.saveProfile(p); }
    @PutMapping("/settings/prefs") public BusinessProfile prefs(@RequestBody SettingsService.Prefs p) { return settings.savePrefs(p); }
    @GetMapping("/settings/ladder") public List<LadderStage> getLadder() { return settings.ladder(); }
    @PutMapping("/settings/ladder") public List<LadderStage> saveLadder(@RequestBody List<LadderStage> l) { return settings.saveLadder(l); }
    @GetMapping("/expenses") public Object expenses() { return settings.expenses(); }

    public record DemoDateReq(LocalDate date) {}
    @PutMapping("/settings/demo-date")
    public Map<String, Object> demoDate(@RequestBody DemoDateReq r) { return Map.of("today", settings.setDemoDate(r.date())); }

    @PostMapping("/demo/reset")
    public Map<String, Object> reset() { settings.resetDemo(); return Map.of("ok", true); }

    // ----- utilities -----
    @GetMapping("/gstin/validate")
    public Map<String, Object> validateGstin(@RequestParam String gstin) {
        boolean ok = GstinValidator.isValid(gstin);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("valid", ok);
        if (ok) { String sc = GstinValidator.stateCode(gstin.trim().toUpperCase()); m.put("stateCode", sc); m.put("stateName", IndianStates.name(sc)); }
        return m;
    }

    @GetMapping("/words")
    public Map<String, String> words(@RequestParam long paise) { return Map.of("words", IndianNumberWords.rupees(paise)); }

    // ----- dashboard -----
    @GetMapping("/dashboard")
    public DashboardService.Dashboard dashboard() { return dashboard.get(ladder.today().followUps().size()); }

    // ----- customers -----
    @GetMapping("/customers") public List<CustomerService.CustomerView> customers() { return customers.list(); }
    @GetMapping("/customers/{id}") public CustomerService.CustomerView customer(@PathVariable Long id) { return customers.get(id); }
    @GetMapping("/customers/{id}/payments") public List<CustomerService.PaymentRecord> customerPayments(@PathVariable Long id) { return customers.paymentRecord(id); }
    @PostMapping("/customers") public CustomerService.CustomerView addCustomer(@RequestBody CustomerService.CustomerReq r) { return customers.save(null, r); }
    @PutMapping("/customers/{id}") public CustomerService.CustomerView editCustomer(@PathVariable Long id, @RequestBody CustomerService.CustomerReq r) { return customers.save(id, r); }

    // ----- invoices -----
    @GetMapping("/invoices") public List<InvoiceService.InvoiceSummary> invoices() { return invoices.list(); }
    @GetMapping("/invoices/{id}") public InvoiceService.Detail invoice(@PathVariable Long id) { return invoices.detail(id); }
    @PostMapping("/invoices/preview") public GstCalculator.Result preview(@RequestBody InvoiceService.InvoiceReq r) { return invoices.preview(r); }
    @PostMapping("/invoices") public InvoiceService.Detail create(@RequestBody InvoiceService.InvoiceReq r) { return invoices.create(r); }
    @PutMapping("/invoices/{id}") public InvoiceService.Detail update(@PathVariable Long id, @RequestBody InvoiceService.InvoiceReq r) { return invoices.update(id, r); }
    @PostMapping("/invoices/{id}/send") public InvoiceService.Detail send(@PathVariable Long id) { return invoices.send(id); }
    @DeleteMapping("/invoices/{id}") public Map<String, Object> deleteDraft(@PathVariable Long id) { invoices.deleteDraft(id); return Map.of("ok", true); }

    public record DateReq(LocalDate date) {}
    @PostMapping("/invoices/{id}/snooze") public Map<String, Object> snooze(@PathVariable Long id, @RequestBody DateReq r) { ladder.snooze(id, r.date()); return Map.of("ok", true); }
    @PostMapping("/invoices/{id}/promise") public Map<String, Object> promise(@PathVariable Long id, @RequestBody DateReq r) { ladder.promise(id, r.date()); return Map.of("ok", true); }

    // ----- follow-ups, chase -----
    @GetMapping("/followups/today") public LadderService.Today today() { return ladder.today(); }

    public record MarkSentReq(List<Long> invoiceIds, String stage, String channel, String message) {}
    @PostMapping("/followups/mark-sent")
    public Map<String, Object> markSent(@RequestBody MarkSentReq r) { ladder.markSent(r.invoiceIds(), r.stage(), r.channel(), r.message()); return Map.of("ok", true); }

    // ----- automatic email reminders -----
    @GetMapping("/autosend/status") public AutoSendService.Status autoSendStatus() { return autoSend.status(); }
    @GetMapping("/autosend/log") public List<SendLog> autoSendLog() { return autoSend.recent(); }
    @PostMapping("/autosend/run") public AutoSendService.RunResult autoSendRun() { return autoSend.run("RUN_NOW"); }
    @PostMapping("/autosend/{id}/retry") public SendLog autoSendRetry(@PathVariable Long id) { return autoSend.retry(id); }
    @PutMapping("/settings/autosend") public AutoSendService.Status autoSendSettings(@RequestBody AutoSendService.SettingsReq r) { return autoSend.saveSettings(r); }

    @GetMapping("/chase") public List<ChaseService.ChaseItem> chase() { return chase.list(); }

    // ----- forecast -----
    @PostMapping("/forecast") public ForecastService.Forecast forecast(@RequestBody(required = false) ForecastService.WhatIf w) { return forecast.forecast(w); }

    // ----- letters -----
    @GetMapping("/letters/demand")
    public LetterService.Letter letter(@RequestParam Long customerId, @RequestParam(required = false) List<Long> invoiceIds,
                                       @RequestParam(defaultValue = "false") boolean includeInterest) {
        return letters.build(customerId, invoiceIds, includeInterest);
    }

    public record LogLetterReq(Long customerId, List<Long> invoiceIds, String channel) {}
    @PostMapping("/letters/demand/log")
    public Map<String, Object> logLetter(@RequestBody LogLetterReq r) { letters.logLetter(r.customerId(), r.invoiceIds(), r.channel()); return Map.of("ok", true); }

    // ----- payments -----
    @GetMapping("/payments") public List<PaymentService.PaymentView> payments() { return payments.list(); }
    @PostMapping(value = "/payments/import", consumes = {"text/csv", "text/plain"})
    public PaymentService.ImportResult importCsv(@RequestBody String csv) { return payments.importCsv(csv); }
    @PostMapping("/payments") public Object addPayment(@RequestBody PaymentService.ManualReq r) { return payments.addManual(r); }
    @PostMapping("/payments/{id}/confirm") public Map<String, Object> confirm(@PathVariable Long id, @RequestBody PaymentService.ConfirmReq r) { payments.confirm(id, r); return Map.of("ok", true); }
    public record NoteReq(String note) {}
    @PostMapping("/payments/{id}/ignore") public Map<String, Object> ignore(@PathVariable Long id, @RequestBody(required = false) NoteReq r) { payments.ignore(id, r == null ? null : r.note()); return Map.of("ok", true); }

    // ----- inbox -----
    @GetMapping("/inbox") public List<InboxService.InboxView> inbox(@RequestParam(defaultValue = "OPEN") String status) { return inbox.list(status); }
    @PostMapping("/inbox/{id}/resolve") public Map<String, Object> resolve(@PathVariable Long id, @RequestBody InboxService.ResolveReq r) { inbox.resolve(id, r); return Map.of("ok", true); }

    // ----- public customer link (no login) -----
    @GetMapping("/public/pay/{token}") public InvoiceService.Detail pay(@PathVariable String token) { return invoices.detailByToken(token); }
    @PostMapping("/public/pay/{token}/paid") public Map<String, Object> claim(@PathVariable String token, @RequestBody InboxService.ClaimReq r) { inbox.claimPaid(token, r); return Map.of("ok", true); }
    @PostMapping("/public/pay/{token}/dispute") public Map<String, Object> dispute(@PathVariable String token, @RequestBody InboxService.DisputeReq r) { inbox.dispute(token, r); return Map.of("ok", true); }
    @PostMapping("/public/pay/{token}/upi") public Map<String, Object> upi(@PathVariable String token) { inbox.simulateUpiPayment(token); return Map.of("ok", true); }
}

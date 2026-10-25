package com.baaki.service;

import com.baaki.model.SendLog;
import java.time.LocalDate;
import java.util.*;

public interface AutoSendService {
    record RunResult(LocalDate date, boolean testMode, int sent, int failed, List<Map<String, String>> skipped) {}

    record Status(boolean configured, String provider, boolean enabled, boolean testMode,
                  String ownerEmail, String previewRecipient, List<SendLog> queue, List<SendLog> recent) {}

    record SettingsReq(Boolean enabled, Boolean testMode, String ownerEmail) {}

    void daily();
    RunResult run(String origin);
    SendLog retry(Long id);
    Status status();
    List<SendLog> recent();
    Status saveSettings(SettingsReq r);
}

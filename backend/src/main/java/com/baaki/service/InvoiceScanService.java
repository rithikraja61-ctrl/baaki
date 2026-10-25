package com.baaki.service;

import com.baaki.dto.ScanResult;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.web.multipart.MultipartFile;
import java.util.*;

public interface InvoiceScanService {
    long MAX_BYTES = 5L * 1024 * 1024;

    record CustomerRef(Long id, String name, String gstin, String stateCode, int creditDays) {}

    record Context(String businessState, String businessGstin, String registrationType,
                   List<Integer> allowedRatesBp, int hsnMinDigits, List<CustomerRef> customers,
                   Set<String> existingNumbers) {}

    Map<String, Object> status();
    ScanResult scan(MultipartFile file);

    static ScanResult build(JsonNode raw, Context ctx) {
        return com.baaki.service.impl.InvoiceScanServiceImpl.build(raw, ctx);
    }
}

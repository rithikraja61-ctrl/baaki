package com.baaki.controller;

import com.baaki.dto.ScanResult;
import com.baaki.service.InvoiceScanService;



import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/** Read an existing paper or PDF invoice. The result only fills the New invoice form; nothing is saved here. */
@RestController
@RequestMapping("/api/invoices/scan")
public class ScanController {
    private final InvoiceScanService scans;

    public ScanController(InvoiceScanService scans) { this.scans = scans; }

    @GetMapping("/status") public Map<String, Object> status() { return scans.status(); }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ScanResult scan(@RequestParam("file") MultipartFile file) { return scans.scan(file); }
}

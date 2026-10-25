package com.baaki.service;

import java.time.LocalDate;

public interface InvoiceNumberService {
    static String financialYearLabel(LocalDate d) {
        return com.baaki.service.impl.InvoiceNumberServiceImpl.financialYearLabel(d);
    }

    static String format(String prefix, String fy, long n) {
        return com.baaki.service.impl.InvoiceNumberServiceImpl.format(prefix, fy, n);
    }

    String next(String prefix, LocalDate documentDate);
}

package com.baaki.service;

import com.baaki.model.BusinessProfile;
import com.baaki.model.Customer;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface LetterService {
    String TITLE = "Payment Demand Letter";

    static String disclaimer(String businessName) {
        return com.baaki.service.impl.LetterServiceImpl.disclaimer(businessName);
    }

    record LetterLine(Long invoiceId, String invoiceNumber, LocalDate invoiceDate, LocalDate dueDate,
                      long amountPaise, long paidPaise, long owedPaise, int daysOverdue, long interestPaise) {}

    record Letter(String title, String disclaimer, LocalDate letterDate, LocalDate payByDate,
                  BusinessProfile business, Customer customer, List<LetterLine> lines, long owedPaise,
                  boolean msmeEligible, String msmeLine, BigDecimal bankRatePercent, BigDecimal interestRatePercent,
                  long interestPaise, boolean includeInterest, long demandPaise, String demandInWords,
                  String upiUri, List<String> nextSteps) {}

    static List<String> nextSteps(boolean msme) {
        return com.baaki.service.impl.LetterServiceImpl.nextSteps(msme);
    }

    Letter build(Long customerId, List<Long> invoiceIds, boolean includeInterest);
    void logLetter(Long customerId, List<Long> invoiceIds, String channel);
}

package com.baaki.service;

import com.baaki.dto.Snapshot;
import com.baaki.model.*;
import java.time.LocalDate;
import java.util.*;

public interface CustomerService {
    record CustomerView(Long id, String name, String contactPerson, String phone, String email, String address,
                        String city, String stateCode, String stateName, String gstin, boolean registered,
                        int creditDays, long owedPaise, long overduePaise, int openInvoices,
                        ReliabilityCalculator.Stats reliability, String preferredLanguage) {}

    record PaymentRecord(String reference, LocalDate dueDate, LocalDate paidDate, long amountPaise,
                         int daysLate, boolean unpaid) {}

    record CustomerReq(String name, String contactPerson, String phone, String email, String address,
                       String city, String stateCode, String gstin, Integer creditDays, String preferredLanguage) {}

    static Map<Long, ReliabilityCalculator.Stats> reliability(Snapshot s) {
        return com.baaki.service.impl.CustomerServiceImpl.reliability(s);
    }

    List<CustomerView> list();
    CustomerView get(Long id);
    List<PaymentRecord> paymentRecord(Long id);
    CustomerView save(Long id, CustomerReq r);
}

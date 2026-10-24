package com.baaki.service;

import com.baaki.domain.InvoiceSequence;
import com.baaki.repo.InvoiceSequenceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.regex.Pattern;

/**
 * Consecutive numbers per financial year (1 Apr - 31 Mar), e.g. INV/26-27/0001.
 * The sequence row is read with SELECT ... FOR UPDATE so two requests never get the same number.
 */
@Service
public class InvoiceNumberService {
    private static final Pattern ALLOWED = Pattern.compile("^[A-Za-z0-9/-]{1,16}$");
    private final InvoiceSequenceRepository repo;

    public InvoiceNumberService(InvoiceSequenceRepository repo) { this.repo = repo; }

    public static String financialYearLabel(LocalDate d) {
        int start = d.getMonthValue() >= 4 ? d.getYear() : d.getYear() - 1;
        return String.format("%02d-%02d", start % 100, (start + 1) % 100);
    }

    public static String format(String prefix, String fy, long n) {
        String s = prefix + "/" + fy + "/" + String.format("%04d", n);
        if (!ALLOWED.matcher(s).matches()) throw new IllegalStateException("Invoice number invalid or longer than 16 characters: " + s);
        return s;
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public String next(String prefix, LocalDate documentDate) {
        String fy = financialYearLabel(documentDate);
        String key = prefix + "/" + fy;
        InvoiceSequence seq = repo.lockByKey(key).orElseGet(() -> {
            InvoiceSequence s = new InvoiceSequence();
            s.seqKey = key;
            s.nextValue = 1;
            repo.saveAndFlush(s);
            return repo.lockByKey(key).orElseThrow();
        });
        long n = seq.nextValue;
        seq.nextValue = n + 1;
        repo.save(seq);
        return format(prefix, fy, n);
    }
}

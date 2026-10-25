package com.baaki;

import com.baaki.service.impl.InvoiceNumberServiceImpl;


import com.baaki.model.InvoiceSequence;
import com.baaki.repository.InvoiceSequenceRepository;
import com.baaki.service.InvoiceNumberService;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class InvoiceNumberServiceTest {
    @Test
    void financialYearRunsAprilToMarch() {
        assertEquals("26-27", InvoiceNumberService.financialYearLabel(LocalDate.of(2026, 4, 1)));
        assertEquals("26-27", InvoiceNumberService.financialYearLabel(LocalDate.of(2027, 3, 31)));
        assertEquals("25-26", InvoiceNumberService.financialYearLabel(LocalDate.of(2026, 3, 31)));
        assertEquals("99-00", InvoiceNumberService.financialYearLabel(LocalDate.of(2099, 12, 1)));
    }

    @Test
    void formatIsAtMost16AllowedCharacters() {
        String n = InvoiceNumberService.format("INV", "26-27", 1);
        assertEquals("INV/26-27/0001", n);
        assertTrue(n.length() <= 16);
        assertTrue(n.matches("[A-Za-z0-9/-]+"));
        assertThrows(IllegalStateException.class, () -> InvoiceNumberService.format("INVOICE", "26-27", 1));
    }

    /** In-memory stand-in for the locked table. */
    private InvoiceSequenceRepository fakeRepo(Map<String, InvoiceSequence> table) {
        InvoiceSequenceRepository repo = mock(InvoiceSequenceRepository.class);
        when(repo.lockByKey(anyString())).thenAnswer(a -> Optional.ofNullable(table.get((String) a.getArgument(0))));
        when(repo.save(any())).thenAnswer(a -> { InvoiceSequence s = a.getArgument(0); table.put(s.seqKey, s); return s; });
        when(repo.saveAndFlush(any())).thenAnswer(a -> { InvoiceSequence s = a.getArgument(0); table.put(s.seqKey, s); return s; });
        return repo;
    }

    @Test
    void numbersAreConsecutiveAndNeverRepeatWithinYearAndRestartNextYear() {
        Map<String, InvoiceSequence> table = new HashMap<>();
        InvoiceNumberService svc = new InvoiceNumberServiceImpl(fakeRepo(table));
        Set<String> seen = new HashSet<>();
        for (int k = 1; k <= 50; k++) {
            String n = svc.next("INV", LocalDate.of(2026, 10, 24));
            assertEquals(String.format("INV/26-27/%04d", k), n);
            assertTrue(seen.add(n));
        }
        assertEquals("INV/27-28/0001", svc.next("INV", LocalDate.of(2027, 4, 1)));
        assertEquals("INV/26-27/0051", svc.next("INV", LocalDate.of(2027, 3, 31)));
        assertEquals("CN/26-27/0001", svc.next("CN", LocalDate.of(2026, 10, 24)));
    }

    @Test
    void usesThePessimisticLockQuery() {
        Map<String, InvoiceSequence> table = new HashMap<>();
        InvoiceSequenceRepository repo = fakeRepo(table);
        new InvoiceNumberServiceImpl(repo).next("INV", LocalDate.of(2026, 10, 24));
        verify(repo, atLeastOnce()).lockByKey("INV/26-27");
    }
}

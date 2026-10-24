package com.baaki;

import com.baaki.money.IndianNumberWords;
import com.baaki.money.Money;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class IndianNumberWordsTest {
    @Test
    void lakhsAndThousands() {
        assertEquals("Rupees One Lakh Eighteen Thousand Only", IndianNumberWords.rupees(1_18_000_00));
        assertEquals("Rupees Eleven Thousand Eight Hundred Only", IndianNumberWords.rupees(11_800_00));
    }

    @Test
    void croresAndOddNumbers() {
        assertEquals("Rupees Two Crore Thirty Four Lakh Fifty Six Thousand Seven Hundred Eighty Nine Only",
                IndianNumberWords.rupees(2_34_56_789_00L));
        assertEquals("Rupees One Hundred One Only", IndianNumberWords.rupees(101_00));
        assertEquals("Rupees Zero Only", IndianNumberWords.rupees(0));
        assertEquals("Rupees Ninety Nine Lakh Ninety Nine Thousand Nine Hundred Ninety Nine Only", IndianNumberWords.rupees(99_99_999_00L));
    }

    @Test
    void paise() {
        assertEquals("Rupees Ten and Fifty Paise Only", IndianNumberWords.rupees(10_50));
    }

    @Test
    void indianGrouping() {
        assertEquals("₹1,00,000.00", Money.format(1_00_000_00));
        assertEquals("₹12,34,567.89", Money.format(12_34_567_89));
        assertEquals("₹999.00", Money.format(999_00));
        assertEquals("-₹1,500.50", Money.format(-1_500_50));
    }
}

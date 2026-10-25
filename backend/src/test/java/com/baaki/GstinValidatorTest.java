package com.baaki;

import com.baaki.service.GstinValidator;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

class GstinValidatorTest {
    @Test
    void knownPublicExampleHasCorrectCheckDigit() {
        assertEquals('V', GstinValidator.checkChar("27AAPFU0939F1Z"));
        assertTrue(GstinValidator.isValid("27AAPFU0939F1ZV"));
        assertTrue(GstinValidator.isValid("27aapfu0939f1zv"));
    }

    @Test
    void rejectsBadPatternAndLength() {
        assertFalse(GstinValidator.isValid(null));
        assertFalse(GstinValidator.isValid(""));
        assertFalse(GstinValidator.isValid("27AAPFU0939F1Z"));
        assertFalse(GstinValidator.isValid("27AAPFU0939F1ZVX"));
        assertFalse(GstinValidator.isValid("2AAAPFU0939F1ZV"));
        assertFalse(GstinValidator.isValid("27AAPFU0939F1YV")); // 14th char must be Z
        assertFalse(GstinValidator.isValid("00AAPFU0939F1ZV")); // state 00
    }

    static List<String> seedGstins() throws IOException {
        try (InputStream in = GstinValidatorTest.class.getResourceAsStream("/db/migration/mysql/V2__seed.sql")) {
            assertNotNull(in, "seed file on classpath");
            String sql = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            Matcher m = Pattern.compile("'(\\d{2}[A-Z]{5}\\d{4}[A-Z][1-9A-Z]Z[0-9A-Z])'").matcher(sql);
            List<String> out = new ArrayList<>();
            while (m.find()) out.add(m.group(1));
            return out;
        }
    }

    @Test
    void everySampleGstinPassesAndAnySingleChangeFails() throws IOException {
        List<String> all = seedGstins();
        assertTrue(all.size() >= 12, "business + 11 registered customers");
        String chars = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        for (String g : all) {
            assertTrue(GstinValidator.isValid(g), g);
            for (int pos = 0; pos < 15; pos++) {
                for (char c : chars.toCharArray()) {
                    if (c == g.charAt(pos)) continue;
                    String bad = g.substring(0, pos) + c + g.substring(pos + 1);
                    assertFalse(GstinValidator.isValid(bad), "changed " + g + " -> " + bad);
                }
            }
        }
    }

    @Test
    void stateCodeIsFirstTwoDigits() {
        assertEquals("24", GstinValidator.stateCode("24AAHFM8812E1ZF"));
    }
}

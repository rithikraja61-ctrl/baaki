package com.baaki.money;

import java.util.regex.Pattern;

/** GSTIN: 2-digit state + 10-char PAN + entity code + 'Z' + check character (mod-36 Luhn variant). */
public final class GstinValidator {
    private GstinValidator() {}

    private static final String CHARS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final Pattern PATTERN =
            Pattern.compile("^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z][1-9A-Z]Z[0-9A-Z]$");

    public static char checkChar(String first14) {
        int sum = 0;
        for (int i = 0; i < 14; i++) {
            int v = CHARS.indexOf(first14.charAt(i));
            if (v < 0) throw new IllegalArgumentException("Invalid character");
            int p = v * (i % 2 == 0 ? 1 : 2);
            sum += p / 36 + p % 36;
        }
        return CHARS.charAt((36 - sum % 36) % 36);
    }

    public static boolean isValid(String gstin) {
        if (gstin == null) return false;
        String g = gstin.trim().toUpperCase();
        if (!PATTERN.matcher(g).matches()) return false;
        int state = Integer.parseInt(g.substring(0, 2));
        if (state < 1 || state > 38 && state != 97 && state != 99) return false;
        return checkChar(g.substring(0, 14)) == g.charAt(14);
    }

    public static String stateCode(String gstin) {
        return gstin == null || gstin.length() < 2 ? null : gstin.substring(0, 2);
    }
}

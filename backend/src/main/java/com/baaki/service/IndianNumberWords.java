package com.baaki.service;

/** "Rupees One Lakh Eighteen Thousand Only" — Indian system (thousand, lakh, crore). */
public final class IndianNumberWords {
    private IndianNumberWords() {}

    private static final String[] ONES = {"", "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight",
            "Nine", "Ten", "Eleven", "Twelve", "Thirteen", "Fourteen", "Fifteen", "Sixteen", "Seventeen",
            "Eighteen", "Nineteen"};
    private static final String[] TENS = {"", "", "Twenty", "Thirty", "Forty", "Fifty", "Sixty", "Seventy",
            "Eighty", "Ninety"};

    private static String twoDigits(int n) {
        if (n < 20) return ONES[n];
        return TENS[n / 10] + (n % 10 != 0 ? " " + ONES[n % 10] : "");
    }

    private static String threeDigits(int n) {
        StringBuilder sb = new StringBuilder();
        if (n >= 100) {
            sb.append(ONES[n / 100]).append(" Hundred");
            if (n % 100 != 0) sb.append(" ");
        }
        if (n % 100 != 0) sb.append(twoDigits(n % 100));
        return sb.toString();
    }

    public static String words(long n) {
        if (n == 0) return "Zero";
        if (n < 0) return "Minus " + words(-n);
        StringBuilder sb = new StringBuilder();
        long crore = n / 1_00_00_000;
        n %= 1_00_00_000;
        if (crore > 0) sb.append(words(crore)).append(" Crore ");
        int lakh = (int) (n / 1_00_000);
        n %= 1_00_000;
        if (lakh > 0) sb.append(twoDigits(lakh)).append(" Lakh ");
        int thousand = (int) (n / 1000);
        n %= 1000;
        if (thousand > 0) sb.append(twoDigits(thousand)).append(" Thousand ");
        if (n > 0) sb.append(threeDigits((int) n));
        return sb.toString().trim();
    }

    public static String rupees(long paise) {
        long r = paise / 100;
        int p = (int) (Math.abs(paise) % 100);
        String s = "Rupees " + words(r);
        if (p > 0) s += " and " + twoDigits(p) + " Paise";
        return s + " Only";
    }
}

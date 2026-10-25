package com.baaki.model;

import java.util.Set;

/** Languages the app, reminders and pay links support. */
public final class Languages {
    public static final Set<String> ALL = Set.of("en", "hi", "mr");
    private Languages() {}
    public static String orEnglish(String l) { return l != null && ALL.contains(l) ? l : "en"; }
}

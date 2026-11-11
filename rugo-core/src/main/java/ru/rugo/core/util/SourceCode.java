package ru.rugo.core.util;

import java.util.regex.Pattern;

/**
 * Санитайзер кода источника перехода (startapp-payload / поле source).
 * Допустимы только {@code A-Z a-z 0-9 _ -} (см. docs/miniapp/MAX-DOCS-CHECK.md §1.3),
 * максимум 512 символов.
 */
public final class SourceCode {

    private static final Pattern DISALLOWED = Pattern.compile("[^A-Za-z0-9_-]");
    private static final int MAX_LENGTH = 512;

    private SourceCode() {
    }

    public static String sanitize(String raw) {
        if (raw == null) {
            return null;
        }
        String value = DISALLOWED.matcher(raw.trim()).replaceAll("");
        if (value.isEmpty()) {
            return null;
        }
        return value.length() > MAX_LENGTH ? value.substring(0, MAX_LENGTH) : value;
    }
}

package ru.rugo.core.util;

import java.util.Optional;

/**
 * Нормализация российского номера в E.164 (+7XXXXXXXXXX).
 * Принимает 10 цифр, либо 11 цифр с ведущими «7»/«8». Прочие форматы — пусто.
 */
public final class PhoneNormalizer {

    private static final String COUNTRY_CODE = "+7";

    private PhoneNormalizer() {
    }

    public static Optional<String> normalize(String raw) {
        if (raw == null) {
            return Optional.empty();
        }
        String digits = raw.replaceAll("\\D", "");
        if (digits.length() == 11 && (digits.charAt(0) == '7' || digits.charAt(0) == '8')) {
            digits = digits.substring(1);
        }
        if (digits.length() != 10) {
            return Optional.empty();
        }
        return Optional.of(COUNTRY_CODE + digits);
    }
}

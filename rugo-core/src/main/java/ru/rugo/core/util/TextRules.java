package ru.rugo.core.util;

import java.util.regex.Pattern;

/**
 * Строгие правила для текстовых полей: разрешаем только безопасные символы
 * (буквы, цифры, пробелы и «безобидную» пунктуацию). Всё остальное — в т.ч.
 * {@code < > & " ' { } ;} и управляющие символы — отклоняется с понятной подсказкой.
 * Единый источник для валидации заявок партнёров, кабинета и админки.
 */
public final class TextRules {

    private TextRules() {
    }

    /** Короткие поля: буквы, цифры, пробел и . , ( ) - / № + (в т.ч. кавычки-ёлочки). */
    public static final Pattern PLAIN =
            Pattern.compile("[\\p{L}\\p{N}\\s.,()\\-/№+«»]+");

    /** Описания и заметки: то же плюс % : и переносы строк. */
    public static final Pattern DESCRIPTION =
            Pattern.compile("[\\p{L}\\p{N}\\s.,()\\-/№+«»%:]+");

    /** Ссылка на фото: локальный путь /uploads/… либо http(s)-URL без пробелов/кавычек/угловых скобок. */
    public static final Pattern IMAGE_URL =
            Pattern.compile("^(/uploads/[A-Za-z0-9/_.-]+|https?://[A-Za-z0-9.\\-]+(:\\d+)?(/[^\\s\"<>]*)?)$");

    public static final String PLAIN_HINT =
            "разрешены буквы, цифры, пробел и символы . , ( ) - / № +";
    public static final String DESCRIPTION_HINT =
            "разрешены буквы, цифры, пробелы и символы . , ( ) - / № + % :";
    public static final String IMAGE_HINT =
            "ссылка вида /uploads/… или https://домен/путь (без пробелов, кавычек и угловых скобок)";

    /** Максимальная длина заметки. */
    public static final int MAX_NOTE = 2000;
}

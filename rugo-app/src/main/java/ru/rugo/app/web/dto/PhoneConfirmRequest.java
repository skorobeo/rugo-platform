package ru.rugo.app.web.dto;

import jakarta.validation.constraints.Size;

/** Подтверждение номера из {@code WebApp.requestContact()} (мини-приложение MAX). */
public record PhoneConfirmRequest(
        @Size(max = 30, message = "Телефон: некорректная длина")
        String phone,
        @Size(max = 30, message = "Некорректная дата подтверждения")
        String contactAuthDate,
        @Size(max = 128, message = "Некорректная подпись")
        String contactHash
) {
}

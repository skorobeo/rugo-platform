package ru.rugo.app.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

import java.util.List;

public record PartnerLeadRequest(
        @Size(max = 60, message = "Имя: до 60 символов")
        String name,
        @Size(max = 30, message = "Телефон: некорректная длина")
        String phone,
        @Size(max = 10, message = "Слишком много вариантов")
        List<@Size(max = 30, message = "Недопустимый вариант") String> offerTypes,
        @Size(max = 20, message = "Недопустимый тип партнёра")
        String partnerType,
        @Size(max = 120, message = "Название компании: до 120 символов")
        String companyName,
        @Size(max = 60, message = "Контактное лицо: до 60 символов")
        String contactName,
        @Size(max = 100, message = "Район: до 100 символов")
        String district,
        @Valid
        LeadDetails details,
        Boolean consent,
        @Size(max = 512, message = "Источник: до 512 символов")
        String source,
        @Size(max = 200, message = "Слишком длинное значение")
        String website,
        @Size(max = 30, message = "Некорректная дата подтверждения")
        String contactAuthDate,
        @Size(max = 128, message = "Некорректная подпись")
        String contactHash
) {
}

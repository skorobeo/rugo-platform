package ru.rugo.admin.web.dto;

import jakarta.validation.constraints.Size;

/** Создание/редактирование мастерской из админки — поля как в партнёрской форме. */
public record AdminWorkshopRequest(
        @Size(max = 150, message = "Название: до 150 символов")
        String name,
        @Size(max = 500, message = "Специализация: до 500 символов")
        String specialization,
        @Size(max = 100, message = "Город: до 100 символов")
        String city,
        @Size(max = 100, message = "Район: до 100 символов")
        String district,
        @Size(max = 30, message = "Телефон: некорректная длина")
        String phone
) {
}

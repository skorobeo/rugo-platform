package ru.rugo.app.web.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

/** Заявка партнёра на изменение/скрытие своего материала (уходит на модерацию). */
public record PartnerMaterialChangeRequest(
        @NotNull(message = "Не указан материал")
        Integer materialId,
        @NotNull(message = "Не указано действие")
        String action,
        @Size(max = 100, message = "Тип: до 100 символов")
        String category,
        @Size(max = 50, message = "Фракция: до 50 символов")
        String fraction,
        @Positive(message = "Цена должна быть больше 0")
        Integer price,
        @Size(max = 20, message = "Неверная единица измерения")
        String unit,
        @Size(max = 20, message = "Неверный способ получения")
        String deliveryType,
        @Size(max = 100, message = "Район: до 100 символов")
        String district,
        @Size(max = 10, message = "Слишком много фото")
        List<@Size(max = 300, message = "Недопустимый путь к фото") String> photos
) {
}

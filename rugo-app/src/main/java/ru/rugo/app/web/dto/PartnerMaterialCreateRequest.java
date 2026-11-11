package ru.rugo.app.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

/** Материал, который партнёр добавляет из личного кабинета (уходит на модерацию). */
public record PartnerMaterialCreateRequest(
        @NotBlank(message = "Выберите тип материала")
        @Size(max = 100, message = "Тип: до 100 символов")
        String category,
        @Size(max = 50, message = "Фракция: до 50 символов")
        String fraction,
        @NotNull(message = "Укажите цену")
        @Positive(message = "Цена должна быть больше 0")
        Integer price,
        @Size(max = 20, message = "Неверная единица измерения")
        String unit,
        @Size(max = 20, message = "Неверный способ получения")
        String deliveryType,
        @Size(max = 100, message = "Район: до 100 символов")
        String district,
        @Size(max = 10, message = "Слишком много фото")
        List<@Size(max = 300, message = "Недопустимый путь к фото") String> photos,
        @Size(max = 20, message = "Неверный тип партнёра")
        String partnerType,
        @Size(max = 120, message = "Название компании: до 120 символов")
        String companyName
) {
}

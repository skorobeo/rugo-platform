package ru.rugo.admin.web.dto;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Создание/редактирование материала из админки — поля как в партнёрской форме,
 * плюс контакты поставщика (имя и телефон).
 */
public record AdminMaterialRequest(
        @Size(max = 100, message = "Неверный тип материала")
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
        @Size(max = 100, message = "Имя поставщика: до 100 символов")
        String supplierName,
        @Size(max = 30, message = "Телефон: некорректная длина")
        String supplierPhone,
        @Size(max = 10, message = "Слишком много фото")
        List<@Size(max = 300, message = "Недопустимый путь к фото") String> imageUrls
) {
}

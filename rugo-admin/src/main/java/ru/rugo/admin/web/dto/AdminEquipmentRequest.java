package ru.rugo.admin.web.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Создание/редактирование техники из админки — поля как в партнёрской форме,
 * плюс контакты владельца (имя и телефон): по телефону создаётся/находится партнёр.
 */
public record AdminEquipmentRequest(
        @Size(max = 50, message = "Неверная категория")
        String category,
        @Size(max = 100, message = "Марка: до 100 символов")
        String brand,
        @Size(max = 100, message = "Модель: до 100 символов")
        String model,
        @Positive(message = "Грузоподъёмность должна быть больше 0")
        Integer capacity,
        @Size(max = 100, message = "Длина стрелы: до 100 символов")
        String boomLength,
        @Positive(message = "Цена должна быть больше 0")
        Integer price,
        @Size(max = 20, message = "Неверная единица цены")
        String priceUnit,
        @Size(max = 100, message = "Район: до 100 символов")
        String district,
        @Size(max = 2000, message = "Описание: до 2000 символов")
        String description,
        @Size(max = 100, message = "Имя владельца: до 100 символов")
        String ownerName,
        @Size(max = 30, message = "Телефон: некорректная длина")
        String ownerPhone,
        @Size(max = 10, message = "Слишком много фото")
        List<@Size(max = 300, message = "Недопустимый путь к фото") String> imageUrls,
        @Min(value = 1900, message = "Год: от 1900")
        @Max(value = 2100, message = "Год: до 2100")
        Integer year
) {
}

package ru.rugo.app.web.dto;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import ru.rugo.core.service.EquipmentAssembler;

import java.util.List;

/** Техника, которую партнёр добавляет/редактирует в личном кабинете. */
public record PartnerEquipmentRequest(
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
        @Size(max = 10, message = "Слишком много фото")
        List<@Size(max = 300, message = "Недопустимый путь к фото") String> imageUrls,
        @Size(max = 20, message = "Неверный тип партнёра")
        String partnerType,
        @Size(max = 120, message = "Название компании: до 120 символов")
        String companyName
) {

    public EquipmentAssembler.Input toInput() {
        return new EquipmentAssembler.Input(
                category, brand, model, capacity, boomLength, price, priceUnit,
                district, description, imageUrls);
    }
}

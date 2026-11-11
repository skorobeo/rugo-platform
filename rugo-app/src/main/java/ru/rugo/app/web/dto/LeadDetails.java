package ru.rugo.app.web.dto;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record LeadDetails(
        @Size(max = 100, message = "Неверная категория")
        String category,
        @Size(max = 100, message = "Марка: до 100 символов")
        String brand,
        @Size(max = 100, message = "Модель: до 100 символов")
        String model,
        @Positive(message = "Грузоподъёмность должна быть больше 0")
        Integer capacity,
        @Size(max = 100, message = "Длина стрелы: до 100 символов")
        String boomLength,
        @Size(max = 2000, message = "Описание: до 2000 символов")
        String description,
        @Size(max = 50, message = "Фракция: до 50 символов")
        String fraction,
        @Positive(message = "Цена должна быть больше 0")
        Integer price,
        @Size(max = 20, message = "Неверная единица цены")
        String priceUnit,
        @Size(max = 20, message = "Неверная единица измерения")
        String unit,
        @Size(max = 20, message = "Неверный способ получения")
        String deliveryType,
        @Size(max = 120, message = "Название компании: до 120 символов")
        String company,
        @Size(max = 100, message = "Город: до 100 символов")
        String city,
        @Size(max = 500, message = "Специализация: до 500 символов")
        String specialization,
        @Size(max = 500, message = "Описание: до 500 символов")
        String need,
        @Size(max = 10, message = "Слишком много фото")
        List<@Size(max = 300, message = "Недопустимый путь к фото") String> photos,
        Boolean noMatch
) {

    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        putIfPresent(map, "category", category);
        putIfPresent(map, "brand", brand);
        putIfPresent(map, "model", model);
        putIfPresent(map, "capacity", capacity);
        putIfPresent(map, "boomLength", boomLength);
        putIfPresent(map, "description", description);
        putIfPresent(map, "fraction", fraction);
        putIfPresent(map, "price", price);
        putIfPresent(map, "priceUnit", priceUnit);
        putIfPresent(map, "unit", unit);
        putIfPresent(map, "deliveryType", deliveryType);
        putIfPresent(map, "company", company);
        putIfPresent(map, "city", city);
        putIfPresent(map, "specialization", specialization);
        putIfPresent(map, "need", need);
        putIfPresent(map, "photos", photos);
        putIfTrue(map, "noMatch", noMatch);
        return map;
    }

    private void putIfPresent(Map<String, Object> map, String key, Object value) {
        if (value != null) {
            map.put(key, value);
        }
    }

    private void putIfTrue(Map<String, Object> map, String key, Boolean value) {
        if (Boolean.TRUE.equals(value)) {
            map.put(key, true);
        }
    }
}

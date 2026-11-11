package ru.rugo.core.service;

import ru.rugo.core.entity.Equipment;
import ru.rugo.core.util.CatalogOptions;
import ru.rugo.core.util.TextRules;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Общая сборка/валидация полей карточки техники. Единый источник правил
 * для кабинета партнёра и админки, чтобы значения и сообщения не расходились.
 * Владелец и статус карточки выставляются вызывающей стороной.
 */
public final class EquipmentAssembler {

    static final int MAX_TEXT = 100;
    static final int MAX_DESCRIPTION = 2000;
    static final int MAX_IMAGE_URL = 300;

    private EquipmentAssembler() {
    }

    /** Входные поля техники (без владельца и статуса). */
    public record Input(
            String category,
            String brand,
            String model,
            Integer capacity,
            String boomLength,
            Integer price,
            String priceUnit,
            String district,
            String description,
            List<String> imageUrls
    ) {
    }

    /** Создаёт новую карточку по входным полям; ошибки валидации пишутся в {@code errors}. */
    public static Equipment assemble(Input in, Map<String, String> errors) {
        Equipment e = new Equipment();
        apply(e, in, errors);
        return e;
    }

    /** Заполняет карточку полями и валидирует их; ошибки — в {@code errors}. */
    public static void apply(Equipment e, Input in, Map<String, String> errors) {
        String category = trim(in.category());
        if (category == null || !CatalogOptions.EQUIPMENT_CATEGORIES.contains(category)) {
            errors.put("category", "Выберите категорию из списка");
        }
        e.setCategory(category);

        String brand = optionalText(in.brand(), "brand", "Марка", errors);
        String model = optionalText(in.model(), "model", "Модель", errors);
        e.setBrand(brand);
        e.setModel(model);
        e.setType(typeOf(brand, model, category));

        Integer capacity = in.capacity();
        if (capacity == null || capacity <= 0) {
            errors.put("capacity", "Введите грузоподъёмность числом больше 0");
        }
        e.setCapacity(capacity == null ? null : capacity + " " + tonnesLabel(capacity));

        e.setBoomLength(optionalText(in.boomLength(), "boomLength", "Длина стрелы", errors));

        if (in.price() == null || in.price() <= 0) {
            errors.put("price", "Укажите цену числом больше 0");
        }
        e.setPrice(in.price());

        String priceUnit = trim(in.priceUnit());
        if (priceUnit == null) {
            priceUnit = "hour";
        } else if (!CatalogOptions.PRICE_UNITS.contains(priceUnit)) {
            errors.put("priceUnit", "Выберите единицу цены из списка");
        }
        e.setPriceType(switch (priceUnit) {
            case "day" -> "DAY";
            case "shift" -> "SHIFT";
            default -> "HOUR";
        });

        String district = trim(in.district());
        if (district != null && !CatalogOptions.DISTRICTS.contains(district)) {
            errors.put("district", "Выберите район из списка");
        }
        e.setLocation(district == null ? "Воронеж" : "Воронеж, " + district);

        e.setDescription(description(in.description(), errors));

        List<String> images = validateImages(in.imageUrls(), errors);
        e.setImageUrl(images.isEmpty() ? null : images.get(0));
        e.setImageUrls(images.isEmpty() ? null : images);
    }

    public static String typeOf(String brand, String model, String category) {
        if (brand != null && model != null) {
            return brand + " " + model;
        }
        if (model != null) {
            return model;
        }
        if (brand != null) {
            return brand;
        }
        return category == null ? "Техника" : category;
    }

    private static String optionalText(String raw, String field, String label, Map<String, String> errors) {
        String value = trim(raw);
        if (value == null) {
            return null;
        }
        if (value.length() > MAX_TEXT || !TextRules.PLAIN.matcher(value).matches()) {
            errors.put(field, label + ": " + TextRules.PLAIN_HINT + " (до " + MAX_TEXT + " символов)");
        }
        return value;
    }

    private static String description(String raw, Map<String, String> errors) {
        String value = trim(raw);
        if (value == null) {
            return null;
        }
        if (value.length() > MAX_DESCRIPTION || !TextRules.DESCRIPTION.matcher(value).matches()) {
            errors.put("description",
                    "Описание: " + TextRules.DESCRIPTION_HINT + " (до " + MAX_DESCRIPTION + " символов)");
        }
        return value;
    }

    private static List<String> validateImages(List<String> raw, Map<String, String> errors) {
        if (raw == null || raw.isEmpty()) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        for (String item : raw) {
            String url = trim(item);
            if (url == null) {
                continue;
            }
            if (url.length() > MAX_IMAGE_URL || !TextRules.IMAGE_URL.matcher(url).matches()) {
                errors.put("imageUrls", "Фото: " + TextRules.IMAGE_HINT);
                return List.of();
            }
            result.add(url);
        }
        if (result.size() > CatalogOptions.MAX_PHOTOS) {
            errors.put("imageUrls", "Не более " + CatalogOptions.MAX_PHOTOS + " фото");
            return List.of();
        }
        return result;
    }

    public static String tonnesLabel(int n) {
        int mod10 = n % 10;
        int mod100 = n % 100;
        if (mod10 == 1 && mod100 != 11) {
            return "тонна";
        }
        if (mod10 >= 2 && mod10 <= 4 && (mod100 < 12 || mod100 > 14)) {
            return "тонны";
        }
        return "тонн";
    }

    private static String trim(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}

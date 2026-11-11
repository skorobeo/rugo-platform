package ru.rugo.core.service;

import ru.rugo.core.entity.Material;
import ru.rugo.core.util.CatalogOptions;
import ru.rugo.core.util.TextRules;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Общая сборка/валидация полей карточки материала. Единый источник правил
 * для кабинета партнёра и админки, чтобы значения и сообщения не расходились.
 * Поставщик и статус карточки выставляются вызывающей стороной.
 */
public final class MaterialAssembler {

    static final int MAX_FRACTION = 50;
    static final int MAX_IMAGE_URL = 300;

    private MaterialAssembler() {
    }

    /** Входные поля материала (без поставщика и статуса). */
    public record Input(
            String category,
            String fraction,
            Integer price,
            String unit,
            String deliveryType,
            String district,
            List<String> imageUrls
    ) {
    }

    /** Создаёт новую карточку по входным полям; ошибки валидации пишутся в {@code errors}. */
    public static Material assemble(Input in, Map<String, String> errors) {
        Material m = new Material();
        apply(m, in, errors);
        return m;
    }

    /** Заполняет карточку полями и валидирует их; ошибки — в {@code errors}. */
    public static void apply(Material m, Input in, Map<String, String> errors) {
        String category = trim(in.category());
        if (category == null || !CatalogOptions.MATERIAL_TYPES.contains(category)) {
            errors.put("category", "Выберите тип материала из списка");
        }
        m.setType(category);

        // Для типов «Для участка и дачи» фракция не нужна.
        String fraction = trim(in.fraction());
        if (category != null && CatalogOptions.MATERIAL_TYPES_WITHOUT_FRACTION.contains(category)) {
            fraction = null;
        } else if (fraction != null
                && (fraction.length() > MAX_FRACTION || !TextRules.PLAIN.matcher(fraction).matches())) {
            errors.put("fraction", "Фракция: " + TextRules.PLAIN_HINT + " (до " + MAX_FRACTION + " символов)");
        }
        m.setFraction(fraction);

        if (in.price() == null || in.price() <= 0) {
            errors.put("price", "Укажите цену числом больше 0");
        }
        m.setPricePerUnit(in.price());

        String unit = trim(in.unit());
        if (unit == null) {
            unit = "тонна";
        } else if (!CatalogOptions.MATERIAL_UNITS.contains(unit)) {
            errors.put("unit", "Выберите единицу измерения из списка");
        }
        m.setUnit(unit);

        String delivery = trim(in.deliveryType());
        if (delivery == null) {
            delivery = Material.DELIVERY_TYPE_DELIVERY;
        } else if (!CatalogOptions.DELIVERY_TYPES.contains(delivery)) {
            errors.put("deliveryType", "Выберите способ получения из списка");
        }
        m.setDeliveryType(delivery);

        String district = trim(in.district());
        if (district != null && !CatalogOptions.DISTRICTS.contains(district)) {
            errors.put("district", "Выберите район из списка");
        }
        m.setLocation(district == null ? "Воронеж" : "Воронеж, " + district);

        m.setAvailable(true);

        List<String> images = validateImages(in.imageUrls(), errors);
        m.setImageUrl(images.isEmpty() ? null : images.get(0));
        m.setImageUrls(images.isEmpty() ? null : images);
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

    private static String trim(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}

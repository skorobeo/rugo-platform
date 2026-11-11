package ru.rugo.admin.web.dto;

import java.util.List;

/** Справочники для форм админки: единый источник вместо дублей во фронтенде. */
public record AdminReferenceDto(
        List<String> equipmentCategories,
        List<String> materialTypes,
        List<String> boomCategories,
        List<String> districts,
        List<String> cities,
        List<String> priceUnits,
        List<String> materialUnits,
        List<String> deliveryTypes
) {
}

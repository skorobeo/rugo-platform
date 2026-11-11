package ru.rugo.app.web.dto;

import ru.rugo.core.entity.Material;

import java.util.List;

/** Публичная карточка материала для мини-приложения. */
public record MaterialDto(
        Long id,
        String type,
        String fraction,
        Integer pricePerUnit,
        String unit,
        String location,
        String deliveryType,
        String deliveryZone,
        Boolean available,
        String imageUrl,
        List<String> imageUrls,
        String supplierName,
        String supplierType,
        String supplierPhone
) {

    public static MaterialDto from(Material material) {
        return new MaterialDto(
                material.getId(),
                material.getType(),
                material.getFraction(),
                material.getPricePerUnit(),
                material.getUnit(),
                material.getLocation(),
                material.getDeliveryType(),
                material.getDeliveryZone(),
                material.getAvailable(),
                material.getImageUrl(),
                material.getImageUrls(),
                material.getSupplier() == null ? null : material.getSupplier().getName(),
                material.getSupplier() == null ? null : material.getSupplier().getPartnerType(),
                material.getSupplier() == null ? null : material.getSupplier().getPhone()
        );
    }
}

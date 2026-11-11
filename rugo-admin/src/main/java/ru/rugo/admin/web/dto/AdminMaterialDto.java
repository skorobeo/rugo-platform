package ru.rugo.admin.web.dto;

import ru.rugo.core.entity.Material;

import java.time.LocalDateTime;
import java.util.List;

/** Материал для веб-админки. */
public record AdminMaterialDto(
        Long id,
        String type,
        String fraction,
        Integer pricePerUnit,
        String unit,
        String location,
        String deliveryType,
        String deliveryZone,
        Boolean available,
        String status,
        String imageUrl,
        List<String> imageUrls,
        String supplierName,
        String supplierPhone,
        LocalDateTime createdAt
) {

    public static AdminMaterialDto from(Material m) {
        return new AdminMaterialDto(
                m.getId(),
                m.getType(),
                m.getFraction(),
                m.getPricePerUnit(),
                m.getUnit(),
                m.getLocation(),
                m.getDeliveryType(),
                m.getDeliveryZone(),
                m.getAvailable(),
                m.getStatus(),
                m.getImageUrl(),
                m.getImageUrls(),
                m.getSupplier() == null ? null : m.getSupplier().getName(),
                m.getSupplier() == null ? null : m.getSupplier().getPhone(),
                m.getCreatedAt()
        );
    }
}

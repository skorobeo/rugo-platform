package ru.rugo.admin.web.dto;

import ru.rugo.core.entity.Equipment;

import java.time.LocalDateTime;
import java.util.List;

/** Техника для веб-админки. */
public record AdminEquipmentDto(
        Long id,
        String category,
        String type,
        String brand,
        String model,
        Integer year,
        String capacity,
        String boomLength,
        String description,
        String location,
        String priceType,
        Integer price,
        String status,
        String imageUrl,
        List<String> imageUrls,
        String ownerName,
        String ownerPhone,
        LocalDateTime createdAt
) {

    public static AdminEquipmentDto from(Equipment e) {
        return new AdminEquipmentDto(
                e.getId(),
                e.getCategory(),
                e.getType(),
                e.getBrand(),
                e.getModel(),
                e.getYear(),
                e.getCapacity(),
                e.getBoomLength(),
                e.getDescription(),
                e.getLocation(),
                e.getPriceType(),
                e.getPrice(),
                e.getStatus() == null ? null : e.getStatus().name(),
                e.getImageUrl(),
                e.getImageUrls(),
                e.getOwner() == null ? null : e.getOwner().getName(),
                e.getOwner() == null ? null : e.getOwner().getPhone(),
                e.getCreatedAt()
        );
    }
}

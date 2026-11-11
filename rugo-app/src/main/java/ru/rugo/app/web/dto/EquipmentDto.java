package ru.rugo.app.web.dto;

import ru.rugo.core.entity.Equipment;
import ru.rugo.core.enums.EquipmentStatus;

import java.util.List;

/**
 * Публичная карточка техники для мини-приложения. Отдаём DTO, а не сущность,
 * чтобы не тянуть ленивые связи и не раскрывать служебные поля (maxId, consent и т.п.).
 */
public record EquipmentDto(
        Long id,
        String category,
        String type,
        String brand,
        String model,
        Integer year,
        String capacity,
        String boomLength,
        String description,
        Integer price,
        String priceType,
        String location,
        EquipmentStatus status,
        String imageUrl,
        List<String> imageUrls,
        String ownerName,
        String ownerType,
        String ownerPhone
) {

    public static EquipmentDto from(Equipment equipment) {
        return new EquipmentDto(
                equipment.getId(),
                equipment.getCategory(),
                equipment.getType(),
                equipment.getBrand(),
                equipment.getModel(),
                equipment.getYear(),
                equipment.getCapacity(),
                equipment.getBoomLength(),
                equipment.getDescription(),
                equipment.getPrice(),
                equipment.getPriceType(),
                equipment.getLocation(),
                equipment.getStatus(),
                equipment.getImageUrl(),
                equipment.getImageUrls(),
                equipment.getOwner() == null ? null : equipment.getOwner().getName(),
                equipment.getOwner() == null ? null : equipment.getOwner().getPartnerType(),
                equipment.getOwner() == null ? null : equipment.getOwner().getPhone()
        );
    }
}

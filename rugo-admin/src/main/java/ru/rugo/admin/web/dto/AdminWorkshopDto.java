package ru.rugo.admin.web.dto;

import ru.rugo.core.entity.Workshop;

import java.time.LocalDateTime;

/** Мастерская для веб-админки. */
public record AdminWorkshopDto(
        Long id,
        String name,
        String specialization,
        String location,
        String phone,
        String status,
        LocalDateTime createdAt
) {

    public static AdminWorkshopDto from(Workshop w) {
        return new AdminWorkshopDto(
                w.getId(),
                w.getName(),
                w.getSpecialization(),
                w.getLocation(),
                w.getPhone(),
                w.getStatus(),
                w.getCreatedAt()
        );
    }
}

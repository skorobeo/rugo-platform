package ru.rugo.app.web.dto;

import ru.rugo.core.entity.Workshop;

/** Публичная карточка мастерской для раздела «Найти ремонт». */
public record WorkshopDto(
        Long id,
        String name,
        String specialization,
        String location,
        String phone
) {

    public static WorkshopDto from(Workshop workshop) {
        return new WorkshopDto(
                workshop.getId(),
                workshop.getName(),
                workshop.getSpecialization(),
                workshop.getLocation(),
                workshop.getPhone()
        );
    }
}

package ru.rugo.admin.web.dto;

import ru.rugo.core.entity.User;

import java.time.LocalDateTime;
import java.util.List;

/** Пользователь для веб-админки. */
public record AdminUserDto(
        Long id,
        String name,
        String phone,
        String role,
        String status,
        Long maxId,
        LocalDateTime createdAt,
        long equipmentCount,
        long materialCount,
        List<String> equipmentCategories,
        boolean isAdmin
) {

    public static AdminUserDto from(
            User u, long equipmentCount, long materialCount, List<String> equipmentCategories, boolean isAdmin) {
        return new AdminUserDto(
                u.getId(),
                u.getName(),
                u.getPhone(),
                u.getRole(),
                u.getStatus(),
                u.getMaxId(),
                u.getCreatedAt(),
                equipmentCount,
                materialCount,
                equipmentCategories,
                isAdmin
        );
    }
}

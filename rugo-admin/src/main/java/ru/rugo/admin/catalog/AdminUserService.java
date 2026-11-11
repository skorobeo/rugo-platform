package ru.rugo.admin.catalog;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.rugo.admin.web.dto.AdminUserDto;
import ru.rugo.common.exception.ApiValidationException;
import ru.rugo.common.exception.EntityNotFoundException;
import ru.rugo.core.entity.User;
import ru.rugo.core.repository.AdminUserRepository;
import ru.rugo.core.repository.EquipmentRepository;
import ru.rugo.core.repository.MaterialRepository;
import ru.rugo.core.repository.UserRepository;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Просмотр и управление пользователями из админки. */
@Service
@RequiredArgsConstructor
public class AdminUserService {

    private static final Set<String> ALLOWED_STATUSES =
            Set.of(User.STATUS_ACTIVE, User.STATUS_VERIFIED, User.STATUS_BLOCKED);

    private final UserRepository userRepository;
    private final EquipmentRepository equipmentRepository;
    private final MaterialRepository materialRepository;
    private final AdminUserRepository adminUserRepository;

    @Transactional(readOnly = true)
    public List<AdminUserDto> list(String role, String status) {
        String r = trim(role);
        String s = trim(status);
        Map<Long, Long> equipmentCounts = toCountMap(equipmentRepository.countsByOwnerId());
        Map<Long, Long> materialCounts = toCountMap(materialRepository.countsBySupplierId());
        Map<Long, Set<String>> categories = new HashMap<>();
        equipmentRepository.findAll().forEach(item -> {
            if (item.getOwner() != null && item.getCategory() != null) {
                categories.computeIfAbsent(item.getOwner().getId(), ignored -> new LinkedHashSet<>())
                        .add(item.getCategory());
            }
        });
        Set<Long> adminMaxIds = new HashSet<>(adminUserRepository.findAllMaxUserIds());
        return userRepository.findForAdmin(r, s).stream()
                .map(u -> AdminUserDto.from(u,
                        equipmentCounts.getOrDefault(u.getId(), 0L),
                        materialCounts.getOrDefault(u.getId(), 0L),
                        categories.getOrDefault(u.getId(), Set.of()).stream().sorted().toList(),
                        u.getMaxId() != null && adminMaxIds.contains(u.getMaxId())))
                .toList();
    }

    private Map<Long, Long> toCountMap(List<Object[]> rows) {
        Map<Long, Long> result = new HashMap<>();
        for (Object[] row : rows) {
            result.put((Long) row[0], ((Number) row[1]).longValue());
        }
        return result;
    }

    @Transactional
    public void setStatus(Long id, String status) {
        String value = trim(status);
        if (value == null || !ALLOWED_STATUSES.contains(value)) {
            throw new ApiValidationException(Map.of("status", "Допустимые статусы: ACTIVE, VERIFIED, BLOCKED"));
        }
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Пользователь #" + id + " не найден"));
        user.setStatus(value);
        userRepository.save(user);
    }

    @Transactional
    public void delete(Long id) {
        if (!userRepository.existsById(id)) {
            throw new EntityNotFoundException("Пользователь #" + id + " не найден");
        }
        userRepository.deleteById(id);
    }

    private String trim(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}

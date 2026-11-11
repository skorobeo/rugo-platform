package ru.rugo.admin.directory;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.rugo.core.repository.AdminUserRepository;

import java.util.List;

/** Список администраторов из БД (единый источник для бота и веб-админки). */
@Service
@RequiredArgsConstructor
public class AdminDirectory {

    private final AdminUserRepository repository;

    public boolean isAdmin(Long maxUserId) {
        return maxUserId != null && repository.existsByMaxUserId(maxUserId);
    }

    public boolean hasAdmins() {
        return repository.count() > 0;
    }

    /** MAX id всех администраторов (кому слать карточки). */
    public List<Long> userIds() {
        return repository.findAllMaxUserIds();
    }
}

package ru.rugo.admin.stats;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.rugo.common.config.AppProperties;
import ru.rugo.core.entity.AnalyticsEvent;
import ru.rugo.core.entity.User;
import ru.rugo.core.repository.AdminUserRepository;
import ru.rugo.core.repository.UserRepository;
import ru.rugo.core.util.AnalyticsHash;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Определяет, кто стоит за analytics-хешем: известный пользователь, MAX-пользователь
 * без профиля или анонимная сессия. Имена и телефоны берём только из таблицы users.
 */
@Service
@RequiredArgsConstructor
public class AdminIdentityResolver {

    public enum Type {
        KNOWN,
        UNKNOWN_MAX,
        ANONYMOUS,
        ADMIN
    }

    public record Identity(String hash, Type type, Long userId, String name, String phone, Long maxId) {

        public boolean isKnown() {
            return type == Type.KNOWN;
        }

        public String label() {
            return switch (type) {
                case KNOWN -> name == null || name.isBlank() ? "Пользователь" : name;
                case UNKNOWN_MAX -> "MAX без профиля";
                case ANONYMOUS -> "Анонимная сессия";
                case ADMIN -> "Админ";
            };
        }
    }

    private final UserRepository userRepository;
    private final AdminUserRepository adminUserRepository;
    private final AppProperties appProperties;

    public Map<String, Identity> resolve(Collection<AnalyticsEvent> events) {
        String salt = appProperties.analyticsSalt();
        Set<String> adminHashes = new HashSet<>();
        for (Long adminId : adminUserRepository.findAllMaxUserIds()) {
            adminHashes.add(AnalyticsHash.userHash(salt, adminId));
        }

        Map<String, User> knownByHash = new HashMap<>();
        for (User user : userRepository.findAll()) {
            if (user.getMaxId() != null) {
                knownByHash.put(AnalyticsHash.userHash(salt, user.getMaxId()), user);
            }
        }

        Map<String, Identity> result = new HashMap<>();
        for (AnalyticsEvent event : events) {
            String hash = event.getUserHash();
            if (hash == null || result.containsKey(hash)) {
                continue;
            }
            if (adminHashes.contains(hash)) {
                result.put(hash, new Identity(hash, Type.ADMIN, null, null, null, null));
                continue;
            }
            User user = knownByHash.get(hash);
            if (user != null) {
                result.put(hash, new Identity(hash, Type.KNOWN, user.getId(), user.getName(),
                        user.getPhone(), user.getMaxId()));
                continue;
            }
            if (isAnonymous(hash, event, salt)) {
                result.put(hash, new Identity(hash, Type.ANONYMOUS, null, null, null, null));
                continue;
            }
            result.put(hash, new Identity(hash, Type.UNKNOWN_MAX, null, null, null, null));
        }
        return result;
    }

    public List<Identity> identities(Collection<AnalyticsEvent> events) {
        return resolve(events).values().stream()
                .filter(identity -> identity.type() != Type.ADMIN)
                .toList();
    }

    private boolean isAnonymous(String hash, AnalyticsEvent event, String salt) {
        if (hash.equals(AnalyticsHash.anonHash(salt))) {
            return true;
        }
        Object session = event.getProps() == null ? null : event.getProps().get("session_id");
        if (session instanceof String sessionId && !sessionId.isBlank()) {
            return hash.equals(AnalyticsHash.sessionHash(salt, sessionId));
        }
        return false;
    }
}

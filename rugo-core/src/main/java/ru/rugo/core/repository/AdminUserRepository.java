package ru.rugo.core.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.rugo.core.entity.AdminUser;

import java.util.List;
import java.util.Optional;

public interface AdminUserRepository extends JpaRepository<AdminUser, Long> {

    boolean existsByMaxUserId(Long maxUserId);

    Optional<AdminUser> findByMaxUserId(Long maxUserId);

    @Query("select a.maxUserId from AdminUser a where a.maxUserId is not null order by a.id")
    List<Long> findAllMaxUserIds();
}

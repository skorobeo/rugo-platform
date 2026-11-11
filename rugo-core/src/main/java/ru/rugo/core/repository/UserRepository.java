package ru.rugo.core.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.rugo.core.entity.User;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByPhone(String phone);

    Optional<User> findByMaxId(Long maxId);

    List<User> findByRoleOrderByIdAsc(String role);

    /** Список для админки: фильтры по роли/статусу выполняются в БД (V-018). */
    @Query("SELECT u FROM User u WHERE (:role IS NULL OR u.role = :role) "
            + "AND (:status IS NULL OR u.status = :status) ORDER BY u.id DESC")
    List<User> findForAdmin(@Param("role") String role, @Param("status") String status);
}

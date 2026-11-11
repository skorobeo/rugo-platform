package ru.rugo.core.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.rugo.core.entity.Workshop;

import java.util.List;

public interface WorkshopRepository extends JpaRepository<Workshop, Long> {

    List<Workshop> findByStatusOrderByCreatedAtAsc(String status);

    /** Список для админки: фильтр по статусу выполняется в БД (V-018). */
    @Query("SELECT w FROM Workshop w WHERE (:status IS NULL OR w.status = :status) ORDER BY w.createdAt DESC")
    List<Workshop> findForAdmin(@Param("status") String status);
}

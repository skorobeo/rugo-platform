package ru.rugo.core.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.rugo.core.entity.PartnerLead;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface PartnerLeadRepository extends JpaRepository<PartnerLead, Long> {

    /**
     * Загружает заявку с блокировкой строки (SELECT … FOR UPDATE). Нужно для модерации:
     * два админа, одновременно одобряющих одну заявку, должны получить ровно одну карточку
     * (V-024). Второй вызов дождётся коммита первого и увидит уже сменённый статус.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select l from PartnerLead l where l.id = :id")
    Optional<PartnerLead> findByIdForUpdate(@Param("id") Long id);

    boolean existsByPhoneAndCreatedAtGreaterThanEqual(String phone, OffsetDateTime since);

    List<PartnerLead> findByStatusAndNotifiedAtIsNullOrderByCreatedAtAsc(String status);

    List<PartnerLead> findByStatusOrderByCreatedAtDesc(String status);

    List<PartnerLead> findAllByOrderByCreatedAtDesc();

    /** Заявки конкретного партнёра (по MAX id) для личного кабинета. */
    List<PartnerLead> findByMaxUserIdOrderByCreatedAtDesc(Long maxUserId);

    long countByStatus(String status);
}

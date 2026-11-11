package ru.rugo.core.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.rugo.core.entity.Equipment;
import ru.rugo.core.enums.EquipmentStatus;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface EquipmentRepository extends JpaRepository<Equipment, Long> {

    List<Equipment> findByCategoryAndStatus(String category, EquipmentStatus status);

    List<Equipment> findByCategoryAndStatusAndLocationContainingIgnoreCase(
            String category, EquipmentStatus status, String location);

    List<Equipment> findByOwnerId(Long ownerId);

    List<Equipment> findByOwnerIdOrderByCreatedAtDesc(Long ownerId);

    @EntityGraph(attributePaths = "owner")
    List<Equipment> findByOwnerIdAndStatusNotOrderByCreatedAtDesc(Long ownerId, EquipmentStatus status);

    @EntityGraph(attributePaths = "owner")
    Optional<Equipment> findByIdAndOwnerId(Long id, Long ownerId);

    long countByOwnerId(Long ownerId);

    List<Equipment> findByStatusOrderByReliabilityDescPriceAsc(EquipmentStatus status);

    List<Equipment> findByStatusOrderByCreatedAtAsc(EquipmentStatus status);

    List<Equipment> findByCategoryAndLocationContainingIgnoreCase(String category, String location);

    @Query("SELECT e FROM Equipment e JOIN FETCH e.owner WHERE e.category = :category AND LOWER(e.location) LIKE LOWER(CONCAT('%', :location, '%'))")
    List<Equipment> findByCategoryWithOwner(String category, String location);

    @Query("SELECT e FROM Equipment e JOIN FETCH e.owner WHERE e.category = :category AND e.status IN :statuses AND LOWER(e.location) LIKE LOWER(CONCAT('%', :location, '%')) AND e.owner.status <> 'BLOCKED'")
    List<Equipment> findByCategoryAndStatusesWithOwner(String category, Collection<EquipmentStatus> statuses, String location);

    @Query("SELECT e FROM Equipment e JOIN FETCH e.owner WHERE e.owner.phone = :phone")
    List<Equipment> findByOwnerPhoneWithOwner(String phone);

    @Query("SELECT e FROM Equipment e JOIN FETCH e.owner WHERE e.owner.phone = :phone "
            + "AND e.status IN :statuses AND e.owner.status <> 'BLOCKED' ORDER BY e.createdAt DESC")
    List<Equipment> findByOwnerPhoneAndStatusesWithOwner(String phone, Collection<EquipmentStatus> statuses);

    @Query("SELECT e.category, COUNT(e) FROM Equipment e WHERE e.status IN :statuses "
            + "AND LOWER(e.location) LIKE LOWER(CONCAT('%', :location, '%')) AND e.owner.status <> 'BLOCKED' "
            + "GROUP BY e.category ORDER BY COUNT(e) DESC")
    List<Object[]> countsByCategory(Collection<EquipmentStatus> statuses, String location);

    /** Список для админки: фильтры по категории/статусу выполняются в БД (V-018). */
    @Query("SELECT e FROM Equipment e WHERE (:category IS NULL OR e.category = :category) "
            + "AND (:status IS NULL OR e.status = :status) ORDER BY e.createdAt DESC")
    List<Equipment> findForAdmin(@Param("category") String category, @Param("status") EquipmentStatus status);

    /** Список для админки с набором статусов (например, только опубликованные). */
    @Query("SELECT e FROM Equipment e WHERE (:category IS NULL OR e.category = :category) "
            + "AND e.status IN :statuses ORDER BY e.createdAt DESC")
    List<Equipment> findForAdminByStatuses(@Param("category") String category,
                                           @Param("statuses") Collection<EquipmentStatus> statuses);

    /** Число карточек по владельцам одним запросом (устраняет N+1 в админке, V-018). */
    @Query("SELECT e.owner.id, COUNT(e) FROM Equipment e GROUP BY e.owner.id")
    List<Object[]> countsByOwnerId();
}

package ru.rugo.core.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.rugo.core.entity.Material;

import java.util.List;

public interface MaterialRepository extends JpaRepository<Material, Long> {

    List<Material> findByTypeContainingIgnoreCaseAndAvailableTrue(String type);

    List<Material> findByAvailableTrueOrderByPricePerUnitAsc();

    List<Material> findBySupplierId(Long supplierId);

    /** Материалы поставщика для личного кабинета (supplier подтягивается сразу), всё кроме скрытых. */
    @Query("SELECT m FROM Material m JOIN FETCH m.supplier WHERE m.supplier.id = :supplierId "
            + "AND m.status <> :status ORDER BY m.createdAt DESC")
    List<Material> findOwnedWithSupplier(@Param("supplierId") Long supplierId, @Param("status") String status);

    long countBySupplierId(Long supplierId);

    List<Material> findByTypeContainingIgnoreCase(String type);

    List<Material> findByStatusOrderByCreatedAtAsc(String status);

    @Query("SELECT m FROM Material m JOIN FETCH m.supplier WHERE CONCAT(m.type, ' ', COALESCE(m.fraction, '')) LIKE CONCAT('%', :type, '%')")
    List<Material> findByTypeWithSupplier(String type);

    @Query("SELECT m FROM Material m JOIN FETCH m.supplier WHERE CONCAT(m.type, ' ', COALESCE(m.fraction, '')) LIKE CONCAT('%', :type, '%') AND m.available = true")
    List<Material> findByTypeAvailableWithSupplier(String type);

    @Query("SELECT m FROM Material m JOIN FETCH m.supplier WHERE CONCAT(m.type, ' ', COALESCE(m.fraction, '')) LIKE CONCAT('%', :type, '%') AND m.status = 'APPROVED' AND m.available = true AND m.supplier.status <> 'BLOCKED'")
    List<Material> findByTypeApprovedAvailableWithSupplier(String type);

    @Query("SELECT m FROM Material m JOIN FETCH m.supplier WHERE m.supplier.phone = :phone")
    List<Material> findBySupplierPhoneWithSupplier(String phone);

    @Query("SELECT m.type, COUNT(m) FROM Material m WHERE m.status = 'APPROVED' AND m.available = true "
            + "AND m.supplier.status <> 'BLOCKED' GROUP BY m.type ORDER BY COUNT(m) DESC")
    List<Object[]> countsByType();

    /** Список для админки: фильтры по типу/статусу выполняются в БД (V-018). */
    @Query("SELECT m FROM Material m WHERE (:type IS NULL OR m.type = :type) "
            + "AND (:status IS NULL OR m.status = :status) ORDER BY m.createdAt DESC")
    List<Material> findForAdmin(@Param("type") String type, @Param("status") String status);

    /** Число материалов по поставщикам одним запросом (устраняет N+1, V-018). */
    @Query("SELECT m.supplier.id, COUNT(m) FROM Material m GROUP BY m.supplier.id")
    List<Object[]> countsBySupplierId();
}

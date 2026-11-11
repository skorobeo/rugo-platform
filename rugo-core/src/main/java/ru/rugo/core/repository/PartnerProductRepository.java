package ru.rugo.core.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.rugo.core.entity.PartnerProduct;

import java.util.List;
import java.util.Optional;

public interface PartnerProductRepository extends JpaRepository<PartnerProduct, Long> {

    Optional<PartnerProduct> findByIdAndActiveTrue(Long id);

    Optional<PartnerProduct> findByExternalKey(String externalKey);

    @Query("SELECT p FROM PartnerProduct p JOIN p.category c "
            + "WHERE c.slug = :categorySlug AND p.active = true")
    Page<PartnerProduct> findPageByCategory(
            @Param("categorySlug") String categorySlug,
            Pageable pageable);

    @Query("SELECT p FROM PartnerProduct p JOIN p.category c "
            + "WHERE c.slug = :categorySlug AND p.active = true")
    List<PartnerProduct> findAllByCategory(@Param("categorySlug") String categorySlug);

    @Query("SELECT p FROM PartnerProduct p JOIN p.category c "
            + "WHERE c.slug = :categorySlug AND p.active = true "
            + "AND LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%'))")
    Page<PartnerProduct> search(
            @Param("categorySlug") String categorySlug,
            @Param("query") String query,
            Pageable pageable);

    @Query("SELECT p FROM PartnerProduct p JOIN p.category c "
            + "WHERE c.slug = :categorySlug AND p.active = true "
            + "AND LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<PartnerProduct> searchAll(
            @Param("categorySlug") String categorySlug,
            @Param("query") String query);

    @Query("SELECT p FROM PartnerProduct p "
            + "WHERE p.category.id = :categoryId AND p.id <> :id AND p.active = true "
            + "ORDER BY p.sortOrder ASC, p.name ASC")
    List<PartnerProduct> findRelated(
            @Param("categoryId") Long categoryId,
            @Param("id") Long id,
            Pageable pageable);

    long countByCategoryIdAndActiveTrue(Long categoryId);

    long countByActiveTrue();
}

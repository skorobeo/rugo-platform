package ru.rugo.core.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.rugo.core.entity.PartnerCategory;

import java.util.List;
import java.util.Optional;

public interface PartnerCategoryRepository extends JpaRepository<PartnerCategory, Long> {

    List<PartnerCategory> findAllByActiveTrueOrderBySortOrderAsc();

    Optional<PartnerCategory> findBySlug(String slug);

    @Query("SELECT p.category.id, COUNT(p) FROM PartnerProduct p "
            + "WHERE p.active = true GROUP BY p.category.id")
    List<Object[]> countsByCategory();
}

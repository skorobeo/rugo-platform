package ru.rugo.core.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.rugo.core.entity.PartnerGroup;

import java.util.List;
import java.util.Optional;

public interface PartnerGroupRepository extends JpaRepository<PartnerGroup, Long> {

    List<PartnerGroup> findAllByActiveTrueOrderBySortOrderAsc();

    Optional<PartnerGroup> findBySlug(String slug);
}

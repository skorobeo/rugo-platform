package ru.rugo.core.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.rugo.core.entity.PartnerCatalog;

public interface PartnerCatalogRepository extends JpaRepository<PartnerCatalog, Long> {
}

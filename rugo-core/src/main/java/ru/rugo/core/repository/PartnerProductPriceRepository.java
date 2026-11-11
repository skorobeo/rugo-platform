package ru.rugo.core.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.rugo.core.entity.PartnerProductPrice;

import java.util.Collection;
import java.util.List;

public interface PartnerProductPriceRepository extends JpaRepository<PartnerProductPrice, Long> {

    List<PartnerProductPrice> findByProductIdOrderByVariantAscTemperatureBandAscValidFromDesc(Long productId);

    List<PartnerProductPrice> findByProductIdInOrderByVariantAscTemperatureBandAscValidFromDesc(
            Collection<Long> productIds);

    void deleteByProductId(Long productId);
}

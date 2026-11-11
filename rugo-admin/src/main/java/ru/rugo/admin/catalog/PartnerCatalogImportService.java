package ru.rugo.admin.catalog;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.rugo.core.entity.PartnerCatalog;
import ru.rugo.core.entity.PartnerCategory;
import ru.rugo.core.entity.PartnerGroup;
import ru.rugo.core.entity.PartnerProduct;
import ru.rugo.core.entity.PartnerProductPrice;
import ru.rugo.core.repository.PartnerCatalogRepository;
import ru.rugo.core.repository.PartnerCategoryRepository;
import ru.rugo.core.repository.PartnerGroupRepository;
import ru.rugo.core.repository.PartnerProductPriceRepository;
import ru.rugo.core.repository.PartnerProductRepository;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Идемпотентный импорт снимка каталога ЖБК из classpath-ресурса.
 * Повторный запуск обновляет карточку, разделы, категории, изделия и цены
 * по стабильным ключам (slug / externalKey), не плодя дубли.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PartnerCatalogImportService {

    static final String SEED_RESOURCE = "zhbk/partner_catalog_2026_10.json";

    private final ObjectMapper objectMapper;
    private final PartnerCatalogRepository catalogRepository;
    private final PartnerGroupRepository groupRepository;
    private final PartnerCategoryRepository categoryRepository;
    private final PartnerProductRepository productRepository;
    private final PartnerProductPriceRepository priceRepository;

    @Transactional
    public ImportResult importSeed() {
        PartnerCatalogSeed seed = readSeed();
        int groups = importGroups(seed.groups() == null ? List.of() : seed.groups());
        Map<String, PartnerCategory> categories = importCategories(
                seed.groups() == null ? List.of() : seed.groups());
        importPartner(seed.partner());
        int products = 0;
        int prices = 0;
        List<PartnerCatalogSeed.ProductSeed> productSeeds =
                seed.products() == null ? List.of() : seed.products();
        for (PartnerCatalogSeed.ProductSeed item : productSeeds) {
            PartnerCategory category = categories.get(item.categorySlug());
            if (category == null) {
                throw new IllegalStateException("Категория не найдена в снимке: " + item.categorySlug());
            }
            products++;
            prices += importProduct(item, category);
        }
        log.info("ZhBK catalog imported: groups={}, categories={}, products={}, prices={}",
                groups, categories.size(), products, prices);
        return new ImportResult(1, groups, categories.size(), products, prices);
    }

    private PartnerCatalogSeed readSeed() {
        ClassPathResource resource = new ClassPathResource(SEED_RESOURCE);
        if (!resource.exists()) {
            throw new IllegalStateException("Не найден ресурс импорта: " + SEED_RESOURCE);
        }
        try (InputStream input = resource.getInputStream()) {
            return objectMapper.readValue(input, PartnerCatalogSeed.class);
        } catch (IOException e) {
            throw new IllegalStateException("Не удалось прочитать снимок каталога: " + SEED_RESOURCE, e);
        }
    }

    private void importPartner(PartnerCatalogSeed.PartnerSeed seed) {
        if (seed == null) {
            return;
        }
        PartnerCatalog catalog = catalogRepository.findById(1L)
                .orElseGet(() -> PartnerCatalog.builder().id(1L).build());
        catalog.setName(seed.name());
        catalog.setSubtitle(seed.subtitle());
        catalog.setPhone(seed.phone());
        catalog.setLogoUrl(seed.logoUrl());
        catalog.setFacts(seed.facts());
        catalog.setClients(seed.clients());
        catalogRepository.save(catalog);
    }

    private int importGroups(List<PartnerCatalogSeed.GroupSeed> seeds) {
        for (PartnerCatalogSeed.GroupSeed seed : seeds) {
            PartnerGroup group = groupRepository.findBySlug(seed.slug())
                    .orElseGet(() -> PartnerGroup.builder().slug(seed.slug()).build());
            group.setTitle(seed.title());
            group.setSortOrder(seed.sortOrder() == null ? 0 : seed.sortOrder());
            group.setActive(true);
            groupRepository.save(group);
        }
        return seeds.size();
    }

    private Map<String, PartnerCategory> importCategories(List<PartnerCatalogSeed.GroupSeed> groups) {
        Map<String, PartnerCategory> result = new HashMap<>();
        for (PartnerCatalogSeed.GroupSeed groupSeed : groups) {
            PartnerGroup group = groupRepository.findBySlug(groupSeed.slug())
                    .orElseThrow(() -> new IllegalStateException("Раздел не найден: " + groupSeed.slug()));
            if (groupSeed.categories() == null) {
                continue;
            }
            for (PartnerCatalogSeed.CategorySeed seed : groupSeed.categories()) {
                PartnerCategory category = categoryRepository.findBySlug(seed.slug())
                        .orElseGet(() -> PartnerCategory.builder().slug(seed.slug()).build());
                category.setGroup(group);
                category.setTitle(seed.title());
                category.setImageUrl(seed.imageUrl());
                category.setSortOrder(seed.sortOrder() == null ? 0 : seed.sortOrder());
                category.setActive(true);
                result.put(seed.slug(), categoryRepository.save(category));
            }
        }
        return result;
    }

    private int importProduct(PartnerCatalogSeed.ProductSeed seed, PartnerCategory category) {
        PartnerProduct product = productRepository.findByExternalKey(seed.externalKey())
                .orElseGet(() -> PartnerProduct.builder().externalKey(seed.externalKey()).build());
        product.setCategory(category);
        product.setName(seed.name());
        product.setSourceUrl(seed.sourceUrl());
        product.setUnit(seed.unit() == null ? "шт" : seed.unit());
        product.setAvailability(seed.availability() == null ? "ON_ORDER" : seed.availability());
        product.setTu(seed.tu());
        product.setWeightT(seed.weightT());
        product.setLengthM(seed.lengthM());
        product.setWidthM(seed.widthM());
        product.setThicknessM(seed.thicknessM());
        product.setVolumeM3(seed.volumeM3());
        product.setImageUrl(seed.imageUrl());
        product.setAttributes(seed.attributes());
        product.setSortOrder(seed.sortOrder() == null ? 0 : seed.sortOrder());
        product.setActive(seed.active() == null || seed.active());
        PartnerProduct saved = productRepository.save(product);

        priceRepository.deleteByProductId(saved.getId());
        priceRepository.flush();
        int count = 0;
        List<PartnerCatalogSeed.PriceSeed> priceSeeds =
                seed.prices() == null ? List.of() : seed.prices();
        for (PartnerCatalogSeed.PriceSeed priceSeed : priceSeeds) {
            PartnerProductPrice price = PartnerProductPrice.builder()
                    .product(saved)
                    .variant(priceSeed.variant())
                    .temperatureBand(priceSeed.temperatureBand())
                    .amount(priceSeed.amount())
                    .priceType(priceSeed.priceType() == null ? PartnerProductPrice.TYPE_FIXED : priceSeed.priceType())
                    .currency(priceSeed.currency() == null ? "RUB" : priceSeed.currency())
                    .unit(priceSeed.unit() == null ? saved.getUnit() : priceSeed.unit())
                    .validFrom(priceSeed.validFrom() == null ? LocalDate.now() : priceSeed.validFrom())
                    .validTo(priceSeed.validTo())
                    .note(priceSeed.note())
                    .build();
            priceRepository.save(price);
            count++;
        }
        return count;
    }

    public record ImportResult(
            int partner,
            int groups,
            int categories,
            int products,
            int prices
    ) {
    }
}

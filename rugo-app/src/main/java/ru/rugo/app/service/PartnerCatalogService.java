package ru.rugo.app.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.rugo.app.web.dto.PartnerCatalogDto;
import ru.rugo.app.web.dto.PartnerCategoryDto;
import ru.rugo.app.web.dto.PartnerGroupDto;
import ru.rugo.app.web.dto.PartnerInfoDto;
import ru.rugo.app.web.dto.PartnerProductDetailDto;
import ru.rugo.app.web.dto.PartnerPriceDto;
import ru.rugo.app.web.dto.PartnerProductDto;
import ru.rugo.app.web.dto.PartnerProductPageDto;
import ru.rugo.common.exception.EntityNotFoundException;
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

import java.math.BigDecimal;
import java.text.Collator;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PartnerCatalogService {

    private static final int DEFAULT_PAGE_SIZE = 24;
    private static final int MAX_PAGE_SIZE = 100;
    private static final int RELATED_LIMIT = 8;

    private final PartnerCatalogRepository catalogRepository;
    private final PartnerGroupRepository groupRepository;
    private final PartnerCategoryRepository categoryRepository;
    private final PartnerProductRepository productRepository;
    private final PartnerProductPriceRepository priceRepository;

    @Transactional(readOnly = true)
    public PartnerCatalogDto getCatalog() {
        PartnerCatalog catalog = catalogRepository.findAll().stream().findFirst().orElse(null);
        PartnerInfoDto info = catalog == null
                ? new PartnerInfoDto("АО «Завод ЖБК»", null, null, null, List.of(), List.of())
                : PartnerInfoDto.from(catalog);
        Map<Long, Long> counts = countsByCategory();
        List<PartnerCategory> categories = categoryRepository.findAllByActiveTrueOrderBySortOrderAsc();
        Map<Long, List<PartnerCategory>> byGroup = categories.stream()
                .filter(category -> category.getGroup() != null)
                .collect(Collectors.groupingBy(category -> category.getGroup().getId()));
        List<PartnerGroupDto> groups = groupRepository.findAllByActiveTrueOrderBySortOrderAsc().stream()
                .map(group -> new PartnerGroupDto(
                        group.getSlug(),
                        group.getTitle(),
                        byGroup.getOrDefault(group.getId(), List.of()).stream()
                                .map(category -> PartnerCategoryDto.from(
                                        category, counts.getOrDefault(category.getId(), 0L)))
                                .toList()))
                .toList();
        return new PartnerCatalogDto(info, groups, productRepository.countByActiveTrue());
    }

    @Transactional(readOnly = true)
    public PartnerProductPageDto getProducts(
            String categorySlug, String query, int page, int size, String sort) {
        String slug = categorySlug == null ? "" : categorySlug.trim();
        PartnerCategory category = categoryRepository.findBySlug(slug)
                .orElseThrow(() -> new EntityNotFoundException("Категория каталога не найдена: " + slug));
        String normalizedQuery = query == null || query.isBlank() ? null : query.trim();
        int safePage = Math.max(page, 0);
        int safeSize = size <= 0 ? DEFAULT_PAGE_SIZE : Math.min(size, MAX_PAGE_SIZE);

        List<PartnerProduct> products = normalizedQuery == null
                ? productRepository.findAllByCategory(category.getSlug())
                : productRepository.searchAll(category.getSlug(), normalizedQuery);
        Map<Long, List<PartnerProductPrice>> prices = pricesByProduct(
                products.stream().map(PartnerProduct::getId).toList());
        List<PartnerProductDto> sorted = products.stream()
                .map(product -> PartnerProductDto.from(
                        product, prices.getOrDefault(product.getId(), List.of())))
                .sorted(comparator(sort))
                .toList();

        int total = sorted.size();
        int maxPage = total == 0 ? 0 : (total - 1) / safeSize;
        int effectivePage = Math.min(safePage, maxPage);
        int from = Math.min(effectivePage * safeSize, total);
        int to = Math.min(from + safeSize, total);
        List<PartnerProductDto> items = from >= total ? List.of() : sorted.subList(from, to);

        return new PartnerProductPageDto(
                PartnerCategoryDto.from(category, total),
                items,
                total,
                effectivePage,
                safeSize
        );
    }

    private Comparator<PartnerProductDto> comparator(String sort) {
        String key = sort == null ? "name" : sort;
        return switch (key) {
            case "priceAsc" -> Comparator.comparing(
                    this::minPrice, Comparator.nullsLast(BigDecimal::compareTo));
            case "priceDesc" -> Comparator.comparing(
                    this::minPrice, Comparator.nullsLast((left, right) -> right.compareTo(left)));
            case "weightAsc" -> Comparator.comparing(
                    PartnerProductDto::weightT, Comparator.nullsLast(BigDecimal::compareTo));
            case "lengthAsc" -> Comparator.comparing(
                    PartnerProductDto::lengthM, Comparator.nullsLast(BigDecimal::compareTo));
            case "widthAsc" -> Comparator.comparing(
                    PartnerProductDto::widthM, Comparator.nullsLast(BigDecimal::compareTo));
            default -> {
                Collator collator = Collator.getInstance(Locale.forLanguageTag("ru"));
                collator.setStrength(Collator.PRIMARY);
                yield Comparator.comparing(PartnerProductDto::name, collator);
            }
        };
    }

    private BigDecimal minPrice(PartnerProductDto product) {
        return product.prices().stream()
                .filter(price -> PartnerProductPrice.TYPE_FIXED.equals(price.priceType()))
                .map(PartnerPriceDto::amount)
                .filter(Objects::nonNull)
                .min(BigDecimal::compareTo)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public PartnerProductDetailDto getProduct(Long id) {
        PartnerProduct product = productRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new EntityNotFoundException("Изделие каталога не найдено: " + id));
        List<PartnerProduct> related = related(product);
        Map<Long, List<PartnerProductPrice>> prices = pricesByProduct(
                java.util.stream.Stream.concat(
                                java.util.stream.Stream.of(product.getId()),
                                related.stream().map(PartnerProduct::getId))
                        .toList());
        return new PartnerProductDetailDto(
                PartnerProductDto.from(product, prices.getOrDefault(product.getId(), List.of())),
                related.stream()
                        .map(item -> PartnerProductDto.from(item, prices.getOrDefault(item.getId(), List.of())))
                        .toList()
        );
    }

    @Transactional(readOnly = true)
    public List<PartnerProductDto> getRelated(Long id) {
        PartnerProduct product = productRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new EntityNotFoundException("Изделие каталога не найдено: " + id));
        List<PartnerProduct> related = related(product);
        Map<Long, List<PartnerProductPrice>> prices = pricesByProduct(
                related.stream().map(PartnerProduct::getId).toList());
        return related.stream()
                .map(item -> PartnerProductDto.from(item, prices.getOrDefault(item.getId(), List.of())))
                .toList();
    }

    private List<PartnerProduct> related(PartnerProduct product) {
        if (product.getCategory() == null) {
            return List.of();
        }
        return productRepository.findRelated(
                product.getCategory().getId(),
                product.getId(),
                PageRequest.of(0, RELATED_LIMIT)
        );
    }

    private Map<Long, Long> countsByCategory() {
        Map<Long, Long> counts = new HashMap<>();
        for (Object[] row : categoryRepository.countsByCategory()) {
            counts.put(((Number) row[0]).longValue(), ((Number) row[1]).longValue());
        }
        return counts;
    }

    private Map<Long, List<PartnerProductPrice>> pricesByProduct(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Map.of();
        }
        return priceRepository.findByProductIdInOrderByVariantAscTemperatureBandAscValidFromDesc(ids).stream()
                .filter(price -> price.getProduct() != null)
                .collect(Collectors.groupingBy(price -> price.getProduct().getId()));
    }
}

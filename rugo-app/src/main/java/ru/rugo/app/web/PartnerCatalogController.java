package ru.rugo.app.web;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.rugo.app.service.PartnerCatalogService;
import ru.rugo.app.web.dto.PartnerCatalogDto;
import ru.rugo.app.web.dto.PartnerProductDetailDto;
import ru.rugo.app.web.dto.PartnerProductDto;
import ru.rugo.app.web.dto.PartnerProductPageDto;

import java.util.List;

/**
 * Публичный каталог АО «Завод ЖБК» для мини-приложения.
 * Только чтение, без initData.
 */
@RestController
@RequestMapping("/api/partner-catalog")
@RequiredArgsConstructor
public class PartnerCatalogController {

    private final PartnerCatalogService partnerCatalogService;

    @GetMapping
    public PartnerCatalogDto catalog() {
        return partnerCatalogService.getCatalog();
    }

    @GetMapping("/categories")
    public PartnerCatalogDto categories() {
        return partnerCatalogService.getCatalog();
    }

    @GetMapping("/items")
    public PartnerProductPageDto items(
            @RequestParam String category,
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "24") int size,
            @RequestParam(defaultValue = "name") String sort) {
        return partnerCatalogService.getProducts(category, query, page, size, sort);
    }

    @GetMapping("/items/{id}")
    public PartnerProductDetailDto item(@PathVariable Long id) {
        return partnerCatalogService.getProduct(id);
    }

    @GetMapping("/items/{id}/related")
    public List<PartnerProductDto> related(@PathVariable Long id) {
        return partnerCatalogService.getRelated(id);
    }
}

package ru.rugo.admin.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.rugo.admin.web.dto.AdminReferenceDto;
import ru.rugo.core.util.CatalogOptions;

import java.util.Comparator;
import java.util.List;

/** Справочники для форм админки. Доступ — {@link AdminAuthFilter}. */
@RestController
@RequestMapping("/admin/api/reference")
public class AdminReferenceController {

    @GetMapping
    public AdminReferenceDto reference() {
        return new AdminReferenceDto(
                sorted(CatalogOptions.EQUIPMENT_CATEGORIES),
                sorted(CatalogOptions.MATERIAL_TYPES),
                sorted(CatalogOptions.BOOM_CATEGORIES),
                sorted(CatalogOptions.DISTRICTS),
                sorted(CatalogOptions.CITIES),
                sorted(CatalogOptions.PRICE_UNITS),
                sorted(CatalogOptions.MATERIAL_UNITS),
                sorted(CatalogOptions.DELIVERY_TYPES)
        );
    }

    private List<String> sorted(java.util.Collection<String> values) {
        return values.stream().sorted(Comparator.naturalOrder()).toList();
    }
}

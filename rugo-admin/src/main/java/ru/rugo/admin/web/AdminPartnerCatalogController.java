package ru.rugo.admin.web;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.rugo.admin.catalog.PartnerCatalogImportService;

/** Служебные операции партнёрского каталога ЖБК. Доступ — {@link AdminAuthFilter}. */
@RestController
@RequestMapping("/admin/api/partner-catalog")
@RequiredArgsConstructor
public class AdminPartnerCatalogController {

    private final PartnerCatalogImportService importService;

    @PostMapping("/import")
    public PartnerCatalogImportService.ImportResult importSeed() {
        return importService.importSeed();
    }
}

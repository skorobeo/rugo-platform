package ru.rugo.admin.web;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.RestController;
import ru.rugo.admin.catalog.AdminWorkshopService;
import ru.rugo.admin.web.dto.AdminWorkshopDto;
import ru.rugo.admin.web.dto.AdminWorkshopRequest;

import java.util.List;
import java.util.Map;

/** CRUD мастерских из админки. */
@RestController
@RequestMapping("/admin/api/workshops")
@RequiredArgsConstructor
public class AdminWorkshopController {

    private final AdminWorkshopService workshopService;

    @GetMapping
    public List<AdminWorkshopDto> list(@RequestParam(required = false) String status) {
        return workshopService.list(status);
    }

    @GetMapping("/{id}")
    public AdminWorkshopDto get(@PathVariable Long id) {
        return workshopService.get(id);
    }

    @PostMapping
    public ResponseEntity<Map<String, Long>> create(@Valid @RequestBody AdminWorkshopRequest request) {
        Long id = workshopService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("id", id));
    }

    @PutMapping("/{id}")
    public Map<String, Long> update(@PathVariable Long id, @Valid @RequestBody AdminWorkshopRequest request) {
        return Map.of("id", workshopService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        workshopService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

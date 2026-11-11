package ru.rugo.admin.moderation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.rugo.admin.catalog.CatalogCacheEvictor;
import ru.rugo.admin.events.AfterCommit;
import ru.rugo.admin.notify.AdminMessenger;
import ru.rugo.core.entity.Equipment;
import ru.rugo.core.entity.Material;
import ru.rugo.core.entity.PartnerLead;
import ru.rugo.core.entity.User;
import ru.rugo.core.entity.Workshop;
import ru.rugo.core.repository.EquipmentRepository;
import ru.rugo.core.repository.MaterialRepository;
import ru.rugo.core.repository.PartnerLeadRepository;
import ru.rugo.core.repository.WorkshopRepository;
import ru.rugo.core.service.PartnerAccountService;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/** Одобрение/отклонение заявок и публикация карточек в каталог. */
@Slf4j
@Service
@RequiredArgsConstructor
public class ModerationService {

    private final PartnerLeadRepository leadRepository;
    private final PartnerAccountService partnerAccountService;
    private final EquipmentRepository equipmentRepository;
    private final MaterialRepository materialRepository;
    private final WorkshopRepository workshopRepository;
    private final LeadToCardMapper mapper;
    private final AdminMessenger messenger;
    private final CatalogCacheEvictor cacheEvictor;

    /** Одобряет заявку: создаёт карточку каталога и партнёра. Возвращает описание результата. */
    @Transactional
    public String approve(Long leadId) {
        PartnerLead lead = leadRepository.findByIdForUpdate(leadId).orElse(null);
        if (lead == null) {
            return "Заявка #" + leadId + " не найдена";
        }
        if (!PartnerLead.STATUS_NEW.equals(lead.getStatus())) {
            return "Заявка #" + leadId + " уже обработана (" + lead.getStatus() + ")";
        }

        String direction = primaryOfferType(lead);
        // Изменение существующей карточки (техника/материал) — применяем к ней, новую не создаём.
        Map<String, Object> details = lead.getDetails();
        if (details != null && details.get("action") instanceof String) {
            if ("materials".equals(direction)) {
                return applyMaterialChange(lead, details);
            }
            if ("equipment".equals(direction)) {
                return applyEquipmentChange(lead, details);
            }
        }
        // Парк техники — отдельный сценарий: карточки заводит менеджер вручную.
        if ("park".equals(direction)) {
            lead.setStatus(PartnerLead.STATUS_ACCEPTED);
            lead.setDecidedAt(OffsetDateTime.now());
            leadRepository.save(lead);
            AfterCommit.run(() -> messenger.notifyPartner(lead.getMaxUserId(),
                    "Спасибо! Менеджер RuGo свяжется с вами по парку техники."));
            log.info("Заявка на парк #{} передана менеджеру", leadId);
            return "парк: менеджер свяжется с партнёром";
        }

        User user = resolveUser(lead);
        Long cardId;
        switch (direction) {
            case "equipment" -> cardId = equipmentRepository.save(mapper.toEquipment(lead, user)).getId();
            case "materials" -> cardId = materialRepository.save(mapper.toMaterial(lead, user)).getId();
            case "repair" -> cardId = workshopRepository.save(mapper.toWorkshop(lead)).getId();
            default -> {
                return "Неизвестное направление: " + direction;
            }
        }

        lead.setStatus(PartnerLead.STATUS_ACCEPTED);
        lead.setDecidedAt(OffsetDateTime.now());
        leadRepository.save(lead);

        AfterCommit.run(() -> {
            messenger.notifyPartner(lead.getMaxUserId(),
                    "Ваше предложение опубликовано в каталоге RuGo. Спасибо!");
            cacheEvictor.evict();
        });
        log.info("Заявка #{} одобрена, карточка {} #{}", leadId, direction, cardId);
        return direction + " #" + cardId;
    }

    @Transactional
    public String reject(Long leadId) {
        PartnerLead lead = leadRepository.findByIdForUpdate(leadId).orElse(null);
        if (lead == null) {
            return "Заявка #" + leadId + " не найдена";
        }
        if (!PartnerLead.STATUS_NEW.equals(lead.getStatus())) {
            return "Заявка #" + leadId + " уже обработана (" + lead.getStatus() + ")";
        }
        lead.setStatus(PartnerLead.STATUS_REJECTED);
        lead.setDecidedAt(OffsetDateTime.now());
        leadRepository.save(lead);
        log.info("Заявка #{} отклонена", leadId);
        return "отклонена";
    }

    /** Возвращает заявку в новые: сбрасывает решение и признак отправки карточки. */
    @Transactional
    public String reopen(Long leadId) {
        PartnerLead lead = leadRepository.findByIdForUpdate(leadId).orElse(null);
        if (lead == null) {
            return "Заявка #" + leadId + " не найдена";
        }
        if (PartnerLead.STATUS_NEW.equals(lead.getStatus())) {
            return "Заявка #" + leadId + " уже в новых";
        }
        lead.setStatus(PartnerLead.STATUS_NEW);
        lead.setDecidedAt(null);
        lead.setNotifiedAt(null);
        leadRepository.save(lead);
        log.info("Заявка #{} возвращена в новые", leadId);
        return "возвращена в новые";
    }

    @Transactional(readOnly = true)
    public String contactInfo(Long leadId) {
        PartnerLead lead = leadRepository.findById(leadId).orElse(null);
        if (lead == null) {
            return "Заявка #" + leadId + " не найдена";
        }
        StringBuilder sb = new StringBuilder("Связаться с партнёром:\n");
        if (lead.getCompanyName() != null && !lead.getCompanyName().isBlank()) {
            sb.append("Компания: ").append(lead.getCompanyName()).append("\n");
        }
        sb.append("Имя: ").append(lead.getName()).append("\n");
        sb.append("Телефон: ").append(lead.getPhone());
        return sb.toString();
    }

    private String applyMaterialChange(PartnerLead lead, Map<String, Object> details) {
        Object mid = details.get("materialId");
        if (!(mid instanceof Integer id)) {
            finish(lead, PartnerLead.STATUS_ACCEPTED);
            return "материалы: не указан материал";
        }
        Material material = materialRepository.findById(id.longValue()).orElse(null);
        if (material == null) {
            finish(lead, PartnerLead.STATUS_ACCEPTED);
            return "материалы: материал #" + id + " не найден";
        }
        User supplier = material.getSupplier();
        if (supplier == null || lead.getMaxUserId() == null
                || !lead.getMaxUserId().equals(supplier.getMaxId())) {
            finish(lead, PartnerLead.STATUS_REJECTED);
            return "материалы: отклонено (не ваш материал)";
        }
        mapper.applyToMaterial(material, lead);
        materialRepository.save(material);
        finish(lead, PartnerLead.STATUS_ACCEPTED);
        AfterCommit.run(() -> {
            messenger.notifyPartner(lead.getMaxUserId(),
                    "Изменения материала опубликованы в каталоге RuGo.");
            cacheEvictor.evict();
        });
        log.info("Заявка-изменение #{} по материалу #{} применена", lead.getId(), material.getId());
        return "materials обновлён #" + material.getId();
    }

    private String applyEquipmentChange(PartnerLead lead, Map<String, Object> details) {
        Object eid = details.get("equipmentId");
        if (!(eid instanceof Integer id)) {
            finish(lead, PartnerLead.STATUS_ACCEPTED);
            return "техника: не указана карточка";
        }
        Equipment equipment = equipmentRepository.findById(id.longValue()).orElse(null);
        if (equipment == null) {
            finish(lead, PartnerLead.STATUS_ACCEPTED);
            return "техника: карточка #" + id + " не найдена";
        }
        User owner = equipment.getOwner();
        if (owner == null || lead.getMaxUserId() == null
                || !lead.getMaxUserId().equals(owner.getMaxId())) {
            finish(lead, PartnerLead.STATUS_REJECTED);
            return "техника: отклонено (не ваша техника)";
        }
        mapper.applyToEquipment(equipment, lead);
        equipmentRepository.save(equipment);
        finish(lead, PartnerLead.STATUS_ACCEPTED);
        AfterCommit.run(() -> {
            messenger.notifyPartner(lead.getMaxUserId(),
                    "Изменения техники опубликованы в каталоге RuGo.");
            cacheEvictor.evict();
        });
        log.info("Заявка-изменение #{} по технике #{} применена", lead.getId(), equipment.getId());
        return "equipment обновлён #" + equipment.getId();
    }

    private void finish(PartnerLead lead, String status) {
        lead.setStatus(status);
        lead.setDecidedAt(OffsetDateTime.now());
        leadRepository.save(lead);
    }

    private User resolveUser(PartnerLead lead) {
        String displayName = lead.getCompanyName() != null && !lead.getCompanyName().isBlank()
                ? lead.getCompanyName()
                : lead.getName();
        return partnerAccountService.resolveOrCreate(
                lead.getMaxUserId(),
                lead.getPhone(),
                displayName,
                lead.getPartnerType(),
                lead.getContactName());
    }

    private String primaryOfferType(PartnerLead lead) {
        List<String> types = lead.getOfferTypes();
        if (types == null || types.isEmpty()) {
            return "";
        }
        if (types.contains("park")) {
            return "park";
        }
        if (types.contains("equipment")) {
            return "equipment";
        }
        if (types.contains("materials")) {
            return "materials";
        }
        if (types.contains("repair")) {
            return "repair";
        }
        return types.get(0);
    }
}

package ru.rugo.app.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.rugo.common.config.AppProperties;
import ru.rugo.common.exception.ApiValidationException;
import ru.rugo.common.exception.DuplicateLeadException;
import ru.rugo.core.entity.Material;
import ru.rugo.core.entity.PartnerLead;
import ru.rugo.core.entity.User;
import ru.rugo.core.repository.EquipmentRepository;
import ru.rugo.core.repository.MaterialRepository;
import ru.rugo.core.repository.PartnerLeadRepository;
import ru.rugo.core.service.PartnerLeadService;
import ru.rugo.core.service.model.CreatePartnerLeadCommand;
import ru.rugo.core.util.CatalogOptions;
import ru.rugo.core.util.PhoneNormalizer;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class PartnerLeadServiceImpl implements PartnerLeadService {

    // Справочники — единый источник в ru.go.core.util.CatalogOptions.
    static final Set<String> OFFER_TYPES = CatalogOptions.OFFER_TYPES;
    static final Set<String> PARTNER_TYPES = CatalogOptions.PARTNER_TYPES;
    static final Set<String> PRICE_UNITS = CatalogOptions.PRICE_UNITS;
    static final Set<String> MATERIAL_UNITS = CatalogOptions.MATERIAL_UNITS;
    static final Set<String> DELIVERY_TYPES = CatalogOptions.DELIVERY_TYPES;
    static final Set<String> DISTRICTS = CatalogOptions.DISTRICTS;

    private static final Pattern NAME = Pattern.compile("^[A-Za-zА-Яа-яЁё -]{2,60}$");
    /**
     * Разрешённые символы свободных текстовых полей деталей (марка, модель, грузоподъёмность,
     * фракция): буквы любых языков, цифры, пробелы и безопасная пунктуация.
     * Всё остальное (в т.ч. {@code < > " ' ` & { } $ \} и управляющие коды) отбивается — так
     * разметку/скрипт в значение не протащить.
     */
    private static final Pattern SAFE_TEXT = Pattern.compile("[\\p{L}\\p{N}\\s.,()\\-/³²№+]+");
    /** Название компании: буквы, цифры, пробел и безопасная пунктуация, включая кавычки-ёлочки. */
    private static final Pattern COMPANY = Pattern.compile("[\\p{L}\\p{N}\\s«»\"'.,()№/+\\-]+");
    /** Описание: как SAFE_TEXT, плюс знаки % : и переносы строк. */
    private static final Pattern DESCRIPTION = Pattern.compile("[\\p{L}\\p{N}\\s.,()\\-/№+«»%:]+");
    private static final int MAX_DESCRIPTION = 2000;
    /** «Опишите проблему» (сценарий «нет подходящего варианта»). */
    private static final int MIN_NEED = 3;
    private static final int MAX_NEED = 500;
    /** Максимум фото на одну карточку (правило платформы). */
    static final int MAX_PHOTOS = CatalogOptions.MAX_PHOTOS;
    /** Допустимый путь к загруженному фото. */
    private static final Pattern PHOTO_PATH = Pattern.compile("^/uploads/[A-Za-z0-9/_.-]+$");
    /** Города работы сервиса. */
    static final Set<String> CITIES = CatalogOptions.CITIES;
    /** Белый список категорий: типы техники + типы материалов. */
    static final Set<String> CATEGORIES = CatalogOptions.CATEGORIES;
    private static final int MAX_TEXT = 120;

    private final PartnerLeadRepository repository;
    private final MaterialRepository materialRepository;
    private final EquipmentRepository equipmentRepository;
    private final AppProperties properties;

    /** Действия по существующей карточке (правка). */
    static final Set<String> CHANGE_ACTIONS = Set.of("update");

    @Override
    @Transactional
    public Long createLead(CreatePartnerLeadCommand command) {
        return createLead(command, true);
    }

    @Override
    @Transactional
    public Long createCabinetLead(CreatePartnerLeadCommand command) {
        return createLead(command, false);
    }

    private Long createLead(CreatePartnerLeadCommand command, boolean checkDuplicate) {
        Map<String, String> errors = new LinkedHashMap<>();

        String phone = PhoneNormalizer.normalize(command.phone()).orElse(null);
        if (phone == null) {
            errors.put("phone", "Неверный номер");
        }

        List<String> offerTypes = command.offerTypes() == null ? List.of() : command.offerTypes();
        if (offerTypes.isEmpty() || !OFFER_TYPES.containsAll(offerTypes)) {
            errors.put("offerTypes", "Выберите хотя бы один вариант");
        }
        boolean isPark = offerTypes.contains("park");

        String partnerType = trimToNull(command.partnerType());
        if (partnerType != null && !PARTNER_TYPES.contains(partnerType)) {
            errors.put("partnerType", "Недопустимый тип партнёра");
            partnerType = null;
        }
        // Парк техники оформляется только компанией; материалы доступны и физлицам.
        boolean company = "company".equals(partnerType) || isPark;
        String storedPartnerType = company ? "company" : "individual";

        String companyName = trimToNull(command.companyName());
        if (companyName != null && (companyName.length() > MAX_TEXT || !COMPANY.matcher(companyName).matches())) {
            errors.put("companyName", "Недопустимое название компании");
            companyName = null;
        }
        if (company && companyName == null) {
            errors.put("companyName", "Укажите название компании");
        }

        String contactName = trimToNull(command.contactName());
        if (contactName != null && !NAME.matcher(contactName).matches()) {
            errors.put("contactName", "Контактное лицо: 2–60 символов, буквы, пробел или дефис");
            contactName = null;
        }

        // Отображаемое имя: у компании контактное лицо необязательно — берём название компании.
        String name = command.name() == null ? null : command.name().trim();
        if (company && (name == null || name.isEmpty())) {
            name = companyName;
        }
        if (name == null || !NAME.matcher(name).matches()) {
            errors.put("name", "Укажите имя: 2–60 символов, буквы, пробел или дефис");
        }

        if (!command.consent()) {
            errors.put("consent", "Нужно согласие на обработку данных");
        }

        String district = trimToNull(command.district());
        if (district != null && !DISTRICTS.contains(district)) {
            errors.put("district", "Выберите район из списка");
        }

        Map<String, Object> details = sanitizeDetails(
                command.details(), errors, offerTypes.contains("repair"), company, companyName, isPark);
        validateOwnedChange(details, command, errors);

        if (!errors.isEmpty()) {
            throw new ApiValidationException(errors);
        }

        OffsetDateTime now = OffsetDateTime.now();
        // Дедупликация по телефону — только для публичной формы (LEAD_DEDUP_MINUTES, по умолчанию 10).
        // В кабинете пользователь уже авторизован: повторные добавления и правки не блокируем.
        int dedupMinutes = properties.rateLimit().leadDedupMinutes();
        if (checkDuplicate && dedupMinutes > 0
                && repository.existsByPhoneAndCreatedAtGreaterThanEqual(phone, now.minusMinutes(dedupMinutes))) {
            throw new DuplicateLeadException();
        }

        PartnerLead lead = PartnerLead.builder()
                .maxUserId(command.maxUserId())
                .name(name)
                .phone(phone)
                .offerTypes(List.copyOf(offerTypes))
                .partnerType(storedPartnerType)
                .companyName(companyName)
                .contactName(contactName)
                .district(district)
                .details(details.isEmpty() ? null : details)
                .source(trimToNull(command.source()))
                .consentAt(now)
                .build();

        PartnerLead saved = repository.save(lead);
        return saved.getId();
    }

    /**
     * Заявка на изменение существующей карточки (техника/материал): проверяем действие
     * и владение. Партнёр может править только свои карточки.
     */
    private void validateOwnedChange(
            Map<String, Object> details,
            CreatePartnerLeadCommand command,
            Map<String, String> errors) {
        if (details.get("action") == null) {
            return;
        }
        if (details.get("materialId") instanceof Integer materialId) {
            Material material = materialRepository.findById(materialId.longValue()).orElse(null);
            if (material == null) {
                errors.put("details.materialId", "Материал не найден");
                return;
            }
            User supplier = material.getSupplier();
            if (supplier == null || command.maxUserId() == null
                    || !command.maxUserId().equals(supplier.getMaxId())) {
                errors.put("details.materialId", "Это не ваш материал");
            }
            return;
        }
        if (details.get("equipmentId") instanceof Integer equipmentId) {
            var equipment = equipmentRepository.findById(equipmentId.longValue()).orElse(null);
            if (equipment == null) {
                errors.put("details.equipmentId", "Техника не найдена");
                return;
            }
            User owner = equipment.getOwner();
            if (owner == null || command.maxUserId() == null
                    || !command.maxUserId().equals(owner.getMaxId())) {
                errors.put("details.equipmentId", "Это не ваша техника");
            }
            return;
        }
        errors.put("details.action", "Не указана карточка для изменения");
    }

    private Map<String, Object> sanitizeDetails(
            Map<String, Object> raw,
            Map<String, String> errors,
            boolean repair,
            boolean company,
            String companyName,
            boolean park) {
        Map<String, Object> details = new LinkedHashMap<>();
        if (raw != null) {
            putCategory(details, raw, errors);
            putString(details, raw, "brand", errors);
            putString(details, raw, "model", errors);
            putPositiveInt(details, raw, "capacity", errors, "Грузоподъёмность должна быть больше 0");
            putString(details, raw, "boomLength", errors);
            putDescription(details, raw, errors);
            putString(details, raw, "fraction", errors);

            putPositiveInt(details, raw, "price", errors, "Цена должна быть больше 0");
            putChoice(details, raw, "priceUnit", PRICE_UNITS, errors, "Неверная единица цены");
            putChoice(details, raw, "unit", MATERIAL_UNITS, errors, "Неверная единица измерения");
            putChoice(details, raw, "deliveryType", DELIVERY_TYPES, errors, "Неверный способ получения");

            putCompany(details, raw, errors);
            putCity(details, raw, errors);
            putString(details, raw, "specialization", errors);
            putNeed(details, raw, errors);
            putPhotos(details, raw, errors);

            putAction(details, raw, errors);
            putPositiveInt(details, raw, "materialId", errors, "Неверный материал");
            putPositiveInt(details, raw, "equipmentId", errors, "Неверная техника");

            putTrue(details, raw, "noMatch");
        }
        // Для типов «Для участка и дачи» фракция не нужна — не сохраняем.
        if (details.get("category") instanceof String category
                && CatalogOptions.MATERIAL_TYPES_WITHOUT_FRACTION.contains(category)) {
            details.remove("fraction");
        }
        // Сценарий «нет подходящего варианта»: обязательно описать, что нужно.
        if (Boolean.TRUE.equals(details.get("noMatch")) && !details.containsKey("need")) {
            errors.put("details.need", "Опишите проблему: 3–500 символов");
        }
        if (repair) {
            if (companyName != null) {
                details.put("company", companyName);
            }
            if (company && !details.containsKey("company")) {
                errors.put("details.company", "Укажите название компании");
            }
            if (!details.containsKey("city")) {
                errors.put("details.city", "Выберите город");
            }
            if (!details.containsKey("specialization")) {
                errors.put("details.specialization", "Укажите специализацию");
            }
        }
        if (park && raw != null) {
            putPositiveInt(details, raw, "units", errors, "Укажите примерное число единиц");
            putString(details, raw, "equipment", errors);
        }
        return details;
    }

    private void putCategory(Map<String, Object> target, Map<String, Object> raw, Map<String, String> errors) {
        Object value = raw.get("category");
        if (value == null) {
            return;
        }
        if (value instanceof String text && CATEGORIES.contains(text)) {
            target.put("category", text);
        } else {
            errors.put("details.category", "Выберите вариант из списка");
        }
    }

    private void putPhotos(Map<String, Object> target, Map<String, Object> raw, Map<String, String> errors) {
        Object value = raw.get("photos");
        if (value == null) {
            return;
        }
        if (!(value instanceof List<?> list)) {
            errors.put("details.photos", "Некорректный список фото");
            return;
        }
        if (list.size() > MAX_PHOTOS) {
            errors.put("details.photos", "Не более " + MAX_PHOTOS + " фото");
            return;
        }
        List<String> photos = new ArrayList<>();
        for (Object item : list) {
            if (!(item instanceof String path) || path.length() > 300 || !PHOTO_PATH.matcher(path).matches()) {
                errors.put("details.photos", "Недопустимый путь к фото");
                return;
            }
            photos.add(path);
        }
        if (!photos.isEmpty()) {
            target.put("photos", photos);
        }
    }

    private void putCompany(Map<String, Object> target, Map<String, Object> raw, Map<String, String> errors) {
        if (!(raw.get("company") instanceof String text)) {
            return;
        }
        String trimmed = text.trim();
        if (trimmed.isEmpty()) {
            return;
        }
        if (trimmed.length() > MAX_TEXT || !COMPANY.matcher(trimmed).matches()) {
            errors.put("details.company", "Недопустимое название компании");
            return;
        }
        target.put("company", trimmed);
    }

    private void putNeed(Map<String, Object> target, Map<String, Object> raw, Map<String, String> errors) {
        Object value = raw.get("need");
        if (!(value instanceof String text)) {
            return;
        }
        String trimmed = text.trim();
        if (trimmed.isEmpty()) {
            return;
        }
        if (trimmed.length() < MIN_NEED || trimmed.length() > MAX_NEED || !DESCRIPTION.matcher(trimmed).matches()) {
            errors.put("details.need",
                    "Опишите проблему: " + MIN_NEED + "–" + MAX_NEED
                            + " символов, буквы, цифры и символы . , ( ) - / № + % :");
            return;
        }
        target.put("need", trimmed);
    }

    private void putDescription(Map<String, Object> target, Map<String, Object> raw, Map<String, String> errors) {
        Object value = raw.get("description");
        if (!(value instanceof String text)) {
            return;
        }
        String trimmed = text.trim();
        if (trimmed.isEmpty()) {
            return;
        }
        if (trimmed.length() > MAX_DESCRIPTION || !DESCRIPTION.matcher(trimmed).matches()) {
            errors.put("details.description",
                    "Описание: буквы, цифры, пробелы и символы . , ( ) - / № + % : (до " + MAX_DESCRIPTION + " символов)");
            return;
        }
        target.put("description", trimmed);
    }

    private void putCity(Map<String, Object> target, Map<String, Object> raw, Map<String, String> errors) {
        Object value = raw.get("city");
        if (value == null) {
            return;
        }
        if (value instanceof String text && CITIES.contains(text)) {
            target.put("city", text);
        } else {
            errors.put("details.city", "Выберите город из списка");
        }
    }

    private void putString(
            Map<String, Object> target,
            Map<String, Object> raw,
            String key,
            Map<String, String> errors) {
        Object value = raw.get(key);
        if (!(value instanceof String text)) {
            return;
        }
        String trimmed = text.trim();
        if (trimmed.isEmpty()) {
            return;
        }
        if (trimmed.length() > MAX_TEXT || !SAFE_TEXT.matcher(trimmed).matches()) {
            errors.put("details." + key, "Недопустимые символы или слишком длинное значение");
            return;
        }
        target.put(key, trimmed);
    }

    private void putPositiveInt(
            Map<String, Object> target,
            Map<String, Object> raw,
            String key,
            Map<String, String> errors,
            String message) {
        Object value = raw.get(key);
        if (value == null) {
            return;
        }
        if (value instanceof Number number && number.doubleValue() > 0) {
            target.put(key, number.intValue());
        } else {
            errors.put("details." + key, message);
        }
    }

    private void putChoice(
            Map<String, Object> target,
            Map<String, Object> raw,
            String key,
            Set<String> allowed,
            Map<String, String> errors,
            String message) {
        Object value = raw.get(key);
        if (value == null) {
            return;
        }
        if (value instanceof String text && allowed.contains(text)) {
            target.put(key, text);
        } else {
            errors.put("details." + key, message);
        }
    }

    private void putTrue(Map<String, Object> target, Map<String, Object> raw, String key) {
        if (Boolean.TRUE.equals(raw.get(key))) {
            target.put(key, true);
        }
    }

    private void putAction(Map<String, Object> target, Map<String, Object> raw, Map<String, String> errors) {
        Object value = raw.get("action");
        if (!(value instanceof String text)) {
            return;
        }
        String trimmed = text.trim();
        if (trimmed.isEmpty()) {
            return;
        }
        if (!CHANGE_ACTIONS.contains(trimmed)) {
            errors.put("details.action", "Неверное действие");
            return;
        }
        target.put("action", trimmed);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}

package ru.rugo.core.util;

import java.util.HashSet;
import java.util.Set;

/**
 * Единый справочник значений каталога: категории техники, типы материалов,
 * единицы, способы получения, районы/города и лимиты.
 *
 * <p>Используется и при валидации заявок партнёров (rugo-app), и при ручном
 * добавлении карточек из админки (rugo-admin), чтобы значения не расходились.
 * Синхронизировано с {@code rugo-webapp/src/content/ru.ts}.
 */
public final class CatalogOptions {

    private CatalogOptions() {
    }

    /** Направления заявки. «park» — отдельная заявка на парк техники (обрабатывается вручную). */
    public static final Set<String> OFFER_TYPES = Set.of("equipment", "materials", "repair", "park");

    /** Тип партнёра: частное лицо или компания. */
    public static final Set<String> PARTNER_TYPES = Set.of("individual", "company");

    /** Категории техники (ru.ts → equipment.categories). */
    public static final Set<String> EQUIPMENT_CATEGORIES = Set.of(
            "Самосвал", "Газель", "Экскаватор", "Экскаватор-погрузчик", "Автобетононасос", "Автокран",
            "Фронтальный погрузчик", "Бульдозер", "Манипулятор", "Автовышка", "Эвакуатор",
            "Бетоновозы", "Мини-погрузчик", "Мини-экскаватор", "Автогрейдер", "Каток дорожный",
            "Асфальтоукладчик", "Длинномер (фура)", "Цементовоз", "Трал (низкорамный)");

    /** Категории, для которых показываем «Длину стрелы» (кран/люлька). */
    public static final Set<String> BOOM_CATEGORIES = Set.of("Манипулятор", "Автовышка");

    /** Типы материалов (ru.ts → materials.types, value). */
    public static final Set<String> MATERIAL_TYPES = Set.of(
            "Песок речной", "Песок карьерный", "Щебень гранитный", "Щебень известняковый",
            "Отсев", "ПГС", "Гравий", "Керамзит", "Асфальтовая крошка",
            "Чернозём", "Перегной", "Грунт", "Торф", "Дрова");

    /**
     * Типы материалов без фракции (группа «Для участка и дачи»): для них фракция
     * не запрашивается и не сохраняется.
     */
    public static final Set<String> MATERIAL_TYPES_WITHOUT_FRACTION = Set.of(
            "Чернозём", "Перегной", "Грунт", "Торф", "Дрова");

    /** Объединённый белый список категорий (техника + материалы) для заявок. */
    public static final Set<String> CATEGORIES;

    static {
        Set<String> all = new HashSet<>(EQUIPMENT_CATEGORIES);
        all.addAll(MATERIAL_TYPES);
        CATEGORIES = Set.copyOf(all);
    }

    /** Тип цены техники (Equipment.priceType). */
    public static final Set<String> PRICE_TYPES = Set.of("HOUR", "DAY", "SHIFT");

    /** Единица цены в деталях заявки (details.priceUnit). */
    public static final Set<String> PRICE_UNITS = Set.of("hour", "day", "shift");

    /** Единица измерения материалов (details.unit). */
    public static final Set<String> MATERIAL_UNITS = Set.of("тонна", "м³");

    /** Способы получения (details.deliveryType). */
    public static final Set<String> DELIVERY_TYPES = Set.of("DELIVERY", "PICKUP", "BOTH");

    /** Белый список районов и населённых пунктов. */
    public static final Set<String> DISTRICTS = Set.of(
            "Железнодорожный", "Коминтерновский", "Ленинский", "Левобережный",
            "Советский", "Центральный", "Рамонь", "Семилуки", "Новая Усмань");

    /** Города работы сервиса. */
    public static final Set<String> CITIES = Set.of("Воронеж");

    /** Максимум фото на одну карточку. */
    public static final int MAX_PHOTOS = 3;
}

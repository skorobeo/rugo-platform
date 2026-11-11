package ru.rugo.app.init;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.rugo.common.config.AppProperties;
import ru.rugo.core.entity.Equipment;
import ru.rugo.core.entity.Material;
import ru.rugo.core.entity.User;
import ru.rugo.core.enums.EquipmentStatus;
import ru.rugo.core.repository.EquipmentRepository;
import ru.rugo.core.repository.MaterialRepository;
import ru.rugo.core.repository.UserRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInit implements CommandLineRunner {

    private final UserRepository userRepository;
    private final EquipmentRepository equipmentRepository;
    private final MaterialRepository materialRepository;
    private final AppProperties appProperties;

    private static final int PROVIDERS = 140;

    private static final Map<String, String[][]> EQUIPMENT = Map.of(
            "Самосвал", new String[][]{
                    {"КамАЗ-55111", "13 т", "1800"},
                    {"КамАЗ-6520", "20 т", "2200"},
                    {"МАЗ-5550", "10 т", "1600"},
                    {"КамАЗ-65115", "15 т", "2000"},
                    {"МАЗ-5516", "20 т", "2100"},
                    {"КамАЗ-45143", "13 т", "1900"},
                    {"Volvo FMX", "20 т", "2600"}},
            "Экскаватор", new String[][]{
                    {"ЭО-2621", "ковш 0.25 м³", "1600"},
                    {"ЭО-4320", "ковш 0.65 м³", "2400"},
                    {"Hitachi ZX200", "ковш 1.0 м³", "3000"},
                    {"JCB JS200", "ковш 0.9 м³", "2900"},
                    {"ЭО-3323", "ковш 0.4 м³", "2000"},
                    {"Caterpillar 320D", "ковш 1.1 м³", "3200"},
                    {"Doosan DX220", "ковш 1.0 м³", "3100"}},
            "Автобетононасос", new String[][]{
                    {"БН 24 м", "24 м", "4500"},
                    {"БН 28 м", "28 м", "5000"},
                    {"БН 30 м", "30 м", "5400"},
                    {"БН 32 м", "32 м", "5800"},
                    {"БН 36 м", "36 м", "6500"},
                    {"БН 40 м", "40 м", "7200"},
                    {"БН 47 м", "47 м", "8000"}},
            "Автокран", new String[][]{
                    {"КС-3577", "16 т", "2800"},
                    {"КС-55713", "25 т", "3200"},
                    {"Ивановец 25 т", "25 т", "3300"},
                    {"КС-5576", "25 т", "3400"},
                    {"КС-45717", "32 т", "3800"},
                    {"Liebherr LTM 1030", "30 т", "4500"},
                    {"XCMG QY50K", "50 т", "5500"}},
            "Фронтальный погрузчик", new String[][]{
                    {"Амкодор 333", "2.5 т", "2100"},
                    {"XCMG LW300K", "3 т", "2200"},
                    {"Челябинец ПК-27", "2.7 т", "2300"},
                    {"Lonking CDM835", "3 т", "2350"},
                    {"SDLG LG936", "3 т", "2400"},
                    {"Volvo L90G", "4 т", "3600"},
                    {"Caterpillar 950H", "5 т", "4200"}},
            "Бульдозер", new String[][]{
                    {"Т-130", "160 л.с.", "2800"},
                    {"Т-170", "220 л.с.", "3000"},
                    {"ЧТЗ Б-170", "240 л.с.", "3100"},
                    {"Б-10М", "250 л.с.", "3200"},
                    {"Shantui SD16", "220 л.с.", "3500"},
                    {"D6R", "200 л.с.", "4000"},
                    {"D8R", "350 л.с.", "5000"}},
            "Манипулятор", new String[][]{
                    {"ГАЗон Next", "2 т", "1300"},
                    {"Hyundai Mighty", "3 т", "1500"},
                    {"Hyundai HD78", "3.5 т", "1600"},
                    {"МАЗ 5550", "4 т", "2100"},
                    {"КАМАЗ 43118", "5 т", "2200"},
                    {"Isuzu Giga", "5 т", "2400"},
                    {"Scania P320", "8 т", "3000"}},
            "Автовышка", new String[][]{
                    {"АПТ-18", "18 м", "1150"},
                    {"ВС-18", "18 м", "1200"},
                    {"ВС-22", "22 м", "1400"},
                    {"АГП-22", "22 м", "1350"},
                    {"ВС-26", "26 м", "1600"},
                    {"АПТ-28", "28 м", "1750"},
                    {"ВС-32", "32 м", "2000"}},
            "Бетоновозы", new String[][]{
                    {"КамАЗ 6x4 миксер", "7 м³", "1600"},
                    {"КамАЗ-5511 миксер", "8 м³", "1700"},
                    {"КамАЗ-6520 миксер", "8 м³", "1800"},
                    {"Шаньдун миксер", "8 м³", "1800"},
                    {"МАЗ 6303 миксер", "9 м³", "1950"},
                    {"Volvo FMX миксер", "10 м³", "2500"},
                    {"Мерседес Arocs", "9 м³", "2600"}},
            "Мини-погрузчик", new String[][]{
                    {"Амкодор 352С", "0.5 т", "1600"},
                    {"Bobcat S175", "0.5 т", "1700"},
                    {"Bobcat S650", "0.6 т", "1800"},
                    {"JCB 3CX", "0.6 т", "1900"},
                    {"XCMG XT650", "0.6 т", "1750"},
                    {"Case SR210", "0.7 т", "2000"},
                    {"Bobcat T770", "0.8 т", "2100"}}
    );

    private static final Map<String, String> EQUIPMENT_IMAGES = Map.of(
            "Самосвал", "/uploads/equipment/samoval.jpg",
            "Экскаватор", "/uploads/equipment/ekskavator.png",
            "Автобетононасос", "/uploads/equipment/avtobetononasos.jpg",
            "Автокран", "/uploads/equipment/avtokran.jpg",
            "Фронтальный погрузчик", "/uploads/equipment/frontalny-pogruzchik.jpg",
            "Бульдозер", "/uploads/equipment/buldozer.jpg",
            "Манипулятор", "/uploads/equipment/manipulyator.jpg",
            "Автовышка", "/uploads/equipment/avtovyshka.jpg",
            "Бетоновозы", "/uploads/equipment/betonovozy.jpg",
            "Мини-погрузчик", "/uploads/equipment/mini-pogruzchik.jpg"
    );

    private static final Map<String, String[][]> MATERIALS = Map.of(
            "Песок речной", new String[][]{{"Песок речной", "", "/uploads/materials/pesok-rechnoy.jpg"}},
            "Песок карьерный", new String[][]{{"Песок карьерный", "", "/uploads/materials/pesok-karyerny.webp"}},
            "Щебень гранитный 5-20", new String[][]{{"Щебень гранитный", "5-20", "/uploads/materials/sheben-granitny-5-20.webp"}},
            "Щебень гранитный 20-40", new String[][]{{"Щебень гранитный", "20-40", "/uploads/materials/sheben-granitny-20-40.png"}},
            "Щебень известняковый", new String[][]{{"Щебень известняковый", "", "/uploads/materials/sheben-izvestnyakovy.webp"}},
            "Отсев гранитный", new String[][]{{"Отсев гранитный", "", "/uploads/materials/otsev-granitny.jpg"}},
            "ПГС", new String[][]{{"ПГС", "", "/uploads/materials/pgs.jpg"}},
            "Гравий", new String[][]{{"Гравий", "", "/uploads/materials/graviy.jpg"}},
            "Керамзит", new String[][]{{"Керамзит", "", "/uploads/materials/keramzit.jpg"}},
            "Асфальтовая крошка", new String[][]{{"Асфальтовая крошка", "", "/uploads/materials/asfaltovaya-kroshka.webp"}}
    );

    private static final Map<String, Integer> MATERIAL_PRICES = Map.of(
            "Песок речной", 320,
            "Песок карьерный", 240,
            "Щебень гранитный", 720,
            "Щебень известняковый", 540,
            "Отсев гранитный", 390,
            "ПГС", 250,
            "Гравий", 560,
            "Керамзит", 880,
            "Асфальтовая крошка", 450
    );

    @Override
    @Transactional
    public void run(String... args) {
        if (!appProperties.seedEnabled()) {
            log.info("Сид-данные отключены (app.seed-enabled=false)");
            return;
        }
        if (userRepository.count() > 0) {
            log.info("База не пустая (users={}), сид-данные пропущены", userRepository.count());
            return;
        }

        List<User> providers = seedProviders();
        seedEquipment(providers);
        seedMaterials(providers);
        log.info("Сид-данные загружены: {} владельцев, {} техники, {} материалов",
                userRepository.count(), equipmentRepository.count(), materialRepository.count());
    }

    private List<User> seedProviders() {
        List<User> providers = new ArrayList<>();
        for (int i = 1; i <= PROVIDERS; i++) {
            providers.add(User.builder()
                    .phone(String.format("+7900%07d", i))
                    .name("Владелец №" + i)
                    .role("PROVIDER")
                    .status("ACTIVE")
                    .build());
        }
        return userRepository.saveAll(providers);
    }

    private void seedEquipment(List<User> providers) {
        int ownerIndex = 0;
        for (Map.Entry<String, String[][]> entry : EQUIPMENT.entrySet()) {
            String category = entry.getKey();
            String imageUrl = EQUIPMENT_IMAGES.get(category);
            String[][] specs = entry.getValue();
            for (int i = 0; i < specs.length; i++) {
                String[] spec = specs[i];
                // Демо: у первой карточки самосвала 3 фото, чтобы проверить галерею/свайп.
                List<String> images = "КамАЗ-55111".equals(spec[0])
                        ? List.of("/uploads/equipment/samoval.jpg",
                                "/uploads/equipment/avtobetononasos.jpg",
                                "/uploads/equipment/betonovozy.jpg")
                        : null;
                equipmentRepository.save(Equipment.builder()
                        .owner(providers.get(ownerIndex++ % providers.size()))
                        .category(category)
                        .type(spec[0])
                        .capacity(spec[1])
                        .price(Integer.parseInt(spec[2]))
                        .priceType("HOUR")
                        .location("Воронеж")
                        .status(statusFor(i))
                        .imageUrl(imageUrl)
                        .imageUrls(images)
                        .build());
            }
        }
    }

    private EquipmentStatus statusFor(int i) {
        return switch (i % 3) {
            case 0 -> EquipmentStatus.AVAILABLE;
            case 1 -> EquipmentStatus.BUSY;
            default -> EquipmentStatus.RESERVED;
        };
    }

    private void seedMaterials(List<User> providers) {
        int supplierIndex = 0;
        for (Map.Entry<String, String[][]> entry : MATERIALS.entrySet()) {
            for (String[] spec : entry.getValue()) {
                int basePrice = MATERIAL_PRICES.get(spec[0]);
                for (int i = 0; i < 7; i++) {
                    materialRepository.save(Material.builder()
                            .supplier(providers.get(supplierIndex++ % providers.size()))
                            .type(spec[0])
                            .fraction(spec[1].isEmpty() ? null : spec[1])
                            .pricePerUnit(basePrice + i * 15)
                            .unit("тонна")
                            .location("Воронеж")
                            .available(true)
                            .deliveryType(deliveryTypeFor(i))
                            .imageUrl(spec[2])
                            .build());
                }
            }
        }
    }

    private String deliveryTypeFor(int i) {
        return switch (i % 3) {
            case 0 -> Material.DELIVERY_TYPE_DELIVERY;
            case 1 -> Material.DELIVERY_TYPE_BOTH;
            default -> Material.DELIVERY_TYPE_PICKUP;
        };
    }
}

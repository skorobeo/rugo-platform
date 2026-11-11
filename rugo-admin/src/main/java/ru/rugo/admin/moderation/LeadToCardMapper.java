package ru.rugo.admin.moderation;

import org.springframework.stereotype.Component;
import ru.rugo.core.entity.Equipment;
import ru.rugo.core.entity.Material;
import ru.rugo.core.entity.PartnerLead;
import ru.rugo.core.entity.User;
import ru.rugo.core.entity.Workshop;
import ru.rugo.core.enums.EquipmentStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Преобразует заявку партнёра в карточку каталога. */
@Component
public class LeadToCardMapper {

    public Equipment toEquipment(PartnerLead lead, User owner) {
        Map<String, Object> d = details(lead);
        return Equipment.builder()
                .owner(owner)
                .category(text(d, "category"))
                .type(type(d))
                .brand(text(d, "brand"))
                .model(text(d, "model"))
                .capacity(capacity(d))
                .boomLength(text(d, "boomLength"))
                .description(text(d, "description"))
                .price(intValue(d, "price"))
                .priceType(priceType(text(d, "priceUnit")))
                .location(location(lead))
                .status(EquipmentStatus.AVAILABLE)
                .imageUrl(firstPhoto(d))
                .imageUrls(photos(d))
                .consent(true)
                .build();
    }

    /** Применяет правку из заявки к существующей карточке материала (одобрено модератором). */
    public void applyToMaterial(Material target, PartnerLead lead) {
        Map<String, Object> d = details(lead);
        String category = text(d, "category");
        if (category != null) {
            target.setType(category);
        }
        target.setFraction(text(d, "fraction"));
        Integer price = intValue(d, "price");
        if (price != null) {
            target.setPricePerUnit(price);
        }
        String unit = text(d, "unit");
        if (unit != null) {
            target.setUnit(unit);
        }
        String delivery = text(d, "deliveryType");
        if (delivery != null) {
            target.setDeliveryType(delivery);
        }
        target.setLocation(location(lead));
        List<String> photos = photos(d);
        if (photos != null) {
            target.setImageUrl(photos.get(0));
            target.setImageUrls(photos);
        }
        target.setStatus(Material.STATUS_APPROVED);
    }

    /** Применяет правку из заявки к существующей карточке техники (одобрено модератором). */
    public void applyToEquipment(Equipment target, PartnerLead lead) {
        Map<String, Object> d = details(lead);
        String category = text(d, "category");
        if (category != null) {
            target.setCategory(category);
        }
        target.setBrand(text(d, "brand"));
        target.setModel(text(d, "model"));
        target.setType(type(d));
        target.setCapacity(capacity(d));
        target.setBoomLength(text(d, "boomLength"));
        target.setDescription(text(d, "description"));
        Integer price = intValue(d, "price");
        if (price != null) {
            target.setPrice(price);
        }
        target.setPriceType(priceType(text(d, "priceUnit")));
        target.setLocation(location(lead));
        List<String> photos = photos(d);
        if (photos != null) {
            target.setImageUrl(photos.get(0));
            target.setImageUrls(photos);
        }
        target.setStatus(EquipmentStatus.AVAILABLE);
    }

    public Material toMaterial(PartnerLead lead, User supplier) {
        Map<String, Object> d = details(lead);
        String unit = text(d, "unit");
        String delivery = text(d, "deliveryType");
        return Material.builder()
                .supplier(supplier)
                .type(text(d, "category"))
                .fraction(text(d, "fraction"))
                .pricePerUnit(intValue(d, "price"))
                .unit(unit == null ? "тонна" : unit)
                .location(location(lead))
                .available(true)
                .deliveryType(delivery == null ? Material.DELIVERY_TYPE_DELIVERY : delivery)
                .status(Material.STATUS_APPROVED)
                .imageUrl(firstPhoto(d))
                .imageUrls(photos(d))
                .consent(true)
                .build();
    }

    public Workshop toWorkshop(PartnerLead lead) {
        Map<String, Object> d = details(lead);
        String name = lead.getCompanyName() != null && !lead.getCompanyName().isBlank()
                ? lead.getCompanyName()
                : text(d, "company");
        String specialization = text(d, "specialization");
        String city = text(d, "city");
        String location;
        if (city == null) {
            location = location(lead);
        } else {
            location = city;
            if (lead.getDistrict() != null && !lead.getDistrict().isBlank()) {
                location = location + ", " + lead.getDistrict();
            }
        }
        return Workshop.builder()
                .name(name == null ? lead.getName() : name)
                .specialization(specialization == null ? "Ремонт спецтехники" : specialization)
                .location(location)
                .phone(lead.getPhone())
                .status(Workshop.STATUS_APPROVED)
                .consent(true)
                .build();
    }

    private String type(Map<String, Object> d) {
        String brand = text(d, "brand");
        String model = text(d, "model");
        if (brand != null && model != null) {
            return brand + " " + model;
        }
        if (model != null) {
            return model;
        }
        if (brand != null) {
            return brand;
        }
        String category = text(d, "category");
        return category == null ? "Техника" : category;
    }

    private String capacity(Map<String, Object> d) {
        Integer value = intValue(d, "capacity");
        if (value == null) {
            return null;
        }
        return value + " " + tonnesLabel(value);
    }

    /** Локация карточки: «Воронеж» или «Воронеж, <район>» (чтобы находилось по локации «Воронеж»). */
    private String location(PartnerLead lead) {
        String district = lead.getDistrict();
        return district == null || district.isBlank() ? "Воронеж" : "Воронеж, " + district;
    }

    private String priceType(String priceUnit) {
        if (priceUnit == null) {
            return "HOUR";
        }
        return switch (priceUnit) {
            case "day" -> "DAY";
            case "shift" -> "SHIFT";
            default -> "HOUR";
        };
    }

    private String firstPhoto(Map<String, Object> d) {
        List<String> photos = photos(d);
        return photos == null || photos.isEmpty() ? null : photos.get(0);
    }

    @SuppressWarnings("unchecked")
    private List<String> photos(Map<String, Object> d) {
        Object value = d.get("photos");
        if (value instanceof List<?> list) {
            List<String> result = new ArrayList<>();
            for (Object item : list) {
                if (item instanceof String s && !s.isBlank()) {
                    result.add(s);
                }
            }
            return result.isEmpty() ? null : result;
        }
        return null;
    }

    private Map<String, Object> details(PartnerLead lead) {
        return lead.getDetails() == null ? Map.of() : lead.getDetails();
    }

    private String text(Map<String, Object> d, String key) {
        Object value = d.get(key);
        if (value instanceof String s && !s.isBlank()) {
            return s;
        }
        return null;
    }

    private Integer intValue(Map<String, Object> d, String key) {
        Object value = d.get(key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        return null;
    }

    static String tonnesLabel(int n) {
        int mod10 = n % 10;
        int mod100 = n % 100;
        if (mod10 == 1 && mod100 != 11) {
            return "тонна";
        }
        if (mod10 >= 2 && mod10 <= 4 && (mod100 < 12 || mod100 > 14)) {
            return "тонны";
        }
        return "тонн";
    }
}

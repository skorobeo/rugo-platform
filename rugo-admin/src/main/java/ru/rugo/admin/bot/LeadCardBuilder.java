package ru.rugo.admin.bot;

import ru.max.botapi.model.AttachmentRequest;
import ru.max.botapi.model.CallbackButton;
import ru.max.botapi.model.ImageAttachmentRequest;
import ru.max.botapi.model.InlineKeyboardAttachment;
import ru.max.botapi.model.InlineKeyboardAttachmentRequest;
import ru.max.botapi.model.PhotoAttachmentRequestPayload;
import ru.rugo.admin.config.AdminProperties;
import ru.rugo.core.entity.PartnerLead;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Формирует «карточку» заявки: текст, фото-вложения и inline-клавиатуру. */
public final class LeadCardBuilder {

    private LeadCardBuilder() {
    }

    public static final String ACTION_APPROVE = "approve";
    public static final String ACTION_REJECT = "reject";
    public static final String ACTION_CONTACT = "contact";

    public static String callbackPayload(Long leadId, String action) {
        return "lead:" + leadId + ":" + action;
    }

    public static String text(PartnerLead lead) {
        Map<String, Object> d = lead.getDetails() == null ? Map.of() : lead.getDetails();
        StringBuilder sb = new StringBuilder();
        sb.append("Новая заявка #").append(lead.getId()).append("\n");
        sb.append("Предлагает: ").append(offerTypes(lead)).append("\n");
        String company = lead.getCompanyName() != null && !lead.getCompanyName().isBlank()
                ? lead.getCompanyName()
                : (d.get("company") instanceof String c ? c : null);
        appendLine(sb, "Компания", company);
        sb.append("Имя: ").append(lead.getName()).append("\n");
        appendLine(sb, "Контактное лицо", lead.getContactName());
        sb.append("Телефон: ").append(lead.getPhone()).append("\n");
        if (d.get("units") instanceof Number units) {
            sb.append("Единиц техники: ").append(units.intValue()).append("\n");
        }
        appendLine(sb, "Техника", d.get("equipment"));
        appendLine(sb, "Специализация", d.get("specialization"));
        appendLine(sb, "Город", d.get("city"));
        appendLine(sb, "Тип", d.get("category"));
        appendLine(sb, "Марка", d.get("brand"));
        appendLine(sb, "Модель", d.get("model"));
        if (d.get("capacity") instanceof Number capacity) {
            sb.append("Грузоподъёмность: ").append(capacity.intValue()).append(" ").append(tonnesLabel(capacity.intValue())).append("\n");
        }
        appendLine(sb, "Фракция", d.get("fraction"));
        if (d.get("price") instanceof Number price) {
            sb.append("Цена: ").append(price.intValue());
            Object unit = d.get("priceUnit") != null ? d.get("priceUnit") : d.get("unit");
            if (unit != null) {
                sb.append(" (").append(unit).append(")");
            }
            sb.append("\n");
        }
        appendLine(sb, "Получение", d.get("deliveryType"));
        appendLine(sb, "Район", lead.getDistrict());
        appendLine(sb, "Источник", lead.getSource());
        if (d.get("noMatch") instanceof Boolean noMatch && noMatch) {
            sb.append("Пометка: нет подходящего варианта — уточнить по телефону\n");
        }
        return sb.toString();
    }

    public static InlineKeyboardAttachmentRequest keyboard(Long leadId) {
        return new InlineKeyboardAttachmentRequest(new InlineKeyboardAttachment.KeyboardPayload(List.of(
                List.of(
                        new CallbackButton("✅ Принять", callbackPayload(leadId, ACTION_APPROVE), null),
                        new CallbackButton("❌ Отклонить", callbackPayload(leadId, ACTION_REJECT), null)
                ),
                List.of(new CallbackButton("📞 Связаться", callbackPayload(leadId, ACTION_CONTACT), null))
        )));
    }

    public static List<AttachmentRequest> photoAttachments(PartnerLead lead, AdminProperties properties) {
        List<AttachmentRequest> attachments = new ArrayList<>();
        List<String> photos = photos(lead.getDetails());
        for (String path : photos) {
            String url = properties.absoluteUrl(path);
            if (url != null) {
                attachments.add(new ImageAttachmentRequest(new PhotoAttachmentRequestPayload(null, url, null)));
            }
        }
        return attachments;
    }

    @SuppressWarnings("unchecked")
    private static List<String> photos(Map<String, Object> details) {
        if (details == null) {
            return List.of();
        }
        Object value = details.get("photos");
        if (value instanceof List<?> list) {
            List<String> result = new ArrayList<>();
            for (Object item : list) {
                if (item instanceof String s && !s.isBlank()) {
                    result.add(s);
                }
            }
            return result;
        }
        return List.of();
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

    private static String offerTypes(PartnerLead lead) {
        List<String> types = lead.getOfferTypes();
        if (types == null || types.isEmpty()) {
            return "не указано";
        }
        return String.join(", ", types.stream().map(LeadCardBuilder::offerLabel).toList());
    }

    private static String offerLabel(String type) {
        return switch (type) {
            case "equipment" -> "Техника";
            case "materials" -> "Материалы";
            case "repair" -> "Мастерская";
            case "park" -> "Парк техники";
            default -> type;
        };
    }

    private static void appendLine(StringBuilder sb, String label, Object value) {
        if (value == null) {
            return;
        }
        String text = String.valueOf(value);
        if (!text.isBlank()) {
            sb.append(label).append(": ").append(text).append("\n");
        }
    }
}

package ru.rugo.app.web.dto;

import ru.rugo.core.entity.PartnerProduct;
import ru.rugo.core.entity.PartnerProductPrice;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record PartnerProductDto(
        Long id,
        String categorySlug,
        String categoryTitle,
        String name,
        String unit,
        String availability,
        BigDecimal priceAmount,
        String priceType,
        String priceLabel,
        String tu,
        BigDecimal weightT,
        BigDecimal lengthM,
        BigDecimal widthM,
        BigDecimal thicknessM,
        BigDecimal volumeM3,
        String imageUrl,
        List<Map<String, String>> attributes,
        List<PartnerPriceDto> prices
) {

    public static PartnerProductDto from(PartnerProduct product, List<PartnerProductPrice> prices) {
        PartnerProductPrice current = currentPrice(prices);
        BigDecimal amount = current == null ? null : current.getAmount();
        String priceType = current == null ? PartnerProductPrice.TYPE_ON_REQUEST : current.getPriceType();
        String unit = current == null ? product.getUnit() : current.getUnit();
        String label = priceLabel(prices, amount, priceType, unit);
        return new PartnerProductDto(
                product.getId(),
                product.getCategory() == null ? null : product.getCategory().getSlug(),
                product.getCategory() == null ? null : product.getCategory().getTitle(),
                product.getName(),
                product.getUnit(),
                product.getAvailability(),
                amount,
                priceType,
                label,
                product.getTu(),
                product.getWeightT(),
                product.getLengthM(),
                product.getWidthM(),
                product.getThicknessM(),
                product.getVolumeM3(),
                product.getImageUrl(),
                product.getAttributes(),
                prices.stream().map(PartnerPriceDto::from).toList()
        );
    }

    private static PartnerProductPrice currentPrice(List<PartnerProductPrice> prices) {
        if (prices == null || prices.isEmpty()) {
            return null;
        }
        return prices.stream()
                .filter(price -> PartnerProductPrice.TYPE_FIXED.equals(price.getPriceType()))
                .findFirst()
                .orElse(prices.get(0));
    }

    private static String priceLabel(
            List<PartnerProductPrice> prices,
            BigDecimal amount,
            String priceType,
            String unit) {
        if (!PartnerProductPrice.TYPE_FIXED.equals(priceType) || amount == null) {
            return "по запросу";
        }
        boolean hasVariants = prices != null && prices.stream()
                .anyMatch(price -> PartnerProductPrice.TYPE_FIXED.equals(price.getPriceType())
                        && price.getVariant() != null);
        BigDecimal min = prices == null ? amount : prices.stream()
                .filter(price -> PartnerProductPrice.TYPE_FIXED.equals(price.getPriceType()))
                .map(PartnerProductPrice::getAmount)
                .filter(value -> value != null)
                .min(BigDecimal::compareTo)
                .orElse(amount);
        String prefix = hasVariants ? "от " : "";
        return prefix + formatMoney(min) + " ₽/" + (unit == null ? "шт" : unit);
    }

    private static String formatMoney(BigDecimal value) {
        String plain = value.stripTrailingZeros().toPlainString();
        int dot = plain.indexOf('.');
        String integer = dot >= 0 ? plain.substring(0, dot) : plain;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < integer.length(); i++) {
            if (i > 0 && (integer.length() - i) % 3 == 0) {
                sb.append(' ');
            }
            sb.append(integer.charAt(i));
        }
        return sb.toString();
    }
}

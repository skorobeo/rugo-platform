package ru.rugo.core.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(schema = "zhbk", name = "product_prices")
@Getter
@Setter
@ToString(exclude = "product")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PartnerProductPrice {

    public static final String TYPE_FIXED = "FIXED";
    public static final String TYPE_ON_REQUEST = "ON_REQUEST";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private PartnerProduct product;

    @Column(length = 120)
    private String variant;

    @Column(name = "temperature_band", length = 20)
    private String temperatureBand;

    @Column(precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "price_type", nullable = false, length = 20)
    @Builder.Default
    private String priceType = TYPE_FIXED;

    @Column(nullable = false, length = 3)
    @Builder.Default
    private String currency = "RUB";

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String unit = "шт";

    @Column(name = "valid_from", nullable = false)
    @Builder.Default
    private LocalDate validFrom = LocalDate.now();

    @Column(name = "valid_to")
    private LocalDate validTo;

    @Column(columnDefinition = "TEXT")
    private String note;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
    }
}

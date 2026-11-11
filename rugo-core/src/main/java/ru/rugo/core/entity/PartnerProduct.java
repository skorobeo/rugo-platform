package ru.rugo.core.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Entity
@Table(schema = "zhbk", name = "products")
@Getter
@Setter
@ToString(exclude = "category")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PartnerProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private PartnerCategory category;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String name;

    @Column(name = "external_key", nullable = false, unique = true, length = 255)
    private String externalKey;

    @Column(name = "source_url", columnDefinition = "TEXT")
    private String sourceUrl;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String unit = "шт";

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String availability = "ON_ORDER";

    @Column(length = 160)
    private String tu;

    @Column(name = "weight_t", precision = 10, scale = 3)
    private BigDecimal weightT;

    @Column(name = "length_m", precision = 8, scale = 3)
    private BigDecimal lengthM;

    @Column(name = "width_m", precision = 8, scale = 3)
    private BigDecimal widthM;

    @Column(name = "thickness_m", precision = 8, scale = 3)
    private BigDecimal thicknessM;

    @Column(name = "volume_m3", precision = 10, scale = 4)
    private BigDecimal volumeM3;

    @Column(name = "image_url", columnDefinition = "TEXT")
    private String imageUrl;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "image_urls")
    private List<String> imageUrls;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "attributes")
    private List<Map<String, String>> attributes;

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private Integer sortOrder = 0;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

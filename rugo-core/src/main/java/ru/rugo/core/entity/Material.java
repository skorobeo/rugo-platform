package ru.rugo.core.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "materials")
@Getter
@Setter
@ToString(exclude = "supplier")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Material {

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_APPROVED = "APPROVED";
    public static final String STATUS_HIDDEN = "HIDDEN";

    public static final String DELIVERY_TYPE_DELIVERY = "DELIVERY";
    public static final String DELIVERY_TYPE_PICKUP = "PICKUP";
    public static final String DELIVERY_TYPE_BOTH = "BOTH";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id", nullable = false)
    private User supplier;

    @Column(nullable = false, length = 100)
    private String type;

    @Column(length = 50)
    private String fraction;

    @Column(name = "price_per_unit")
    private Integer pricePerUnit;

    @Column(length = 20)
    @Builder.Default
    private String unit = "тонна";

    @Column(length = 255)
    @Builder.Default
    private String location = "Воронеж";

    @Builder.Default
    private Boolean available = true;

    @Column(name = "delivery_type", length = 20)
    @Builder.Default
    private String deliveryType = "PICKUP";

    @Column(name = "delivery_zone", length = 255)
    private String deliveryZone;

    @Column(length = 20)
    @Builder.Default
    private String status = STATUS_APPROVED;

    @Builder.Default
    private Boolean consent = false;

    @Column(name = "image_url", columnDefinition = "TEXT")
    private String imageUrl;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "image_urls")
    private List<String> imageUrls;

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

package ru.rugo.core.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "equipment")
@Getter
@Setter
@ToString(exclude = "owner")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Equipment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(nullable = false, length = 50)
    private String category;

    @Column(nullable = false, length = 100)
    private String type;

    @Column(length = 100)
    private String brand;

    @Column(length = 100)
    private String model;

    private Integer year;

    @Column(length = 100)
    private String capacity;

    /** Длина стрелы (для манипуляторов/вышек). */
    @Column(name = "boom_length", length = 100)
    private String boomLength;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 255)
    @Builder.Default
    private String location = "Воронеж";

    @Column(name = "price_type", length = 20)
    @Builder.Default
    private String priceType = "HOUR";

    private Integer price;

    @Builder.Default
    private Boolean consent = false;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    @Builder.Default
    private ru.rugo.core.enums.EquipmentStatus status = ru.rugo.core.enums.EquipmentStatus.AVAILABLE;

    @Column(columnDefinition = "DECIMAL(3,2)")
    private BigDecimal rating;

    @Column(columnDefinition = "NUMERIC(4,1)")
    @Builder.Default
    private BigDecimal reliability = new BigDecimal("99.9");

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

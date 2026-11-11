package ru.rugo.core.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@Setter
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    public static final String ROLE_PROVIDER = "PROVIDER";
    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_VERIFIED = "VERIFIED";
    public static final String STATUS_BLOCKED = "BLOCKED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(length = 20, nullable = false)
    private String phone;

    @Column(length = 255, nullable = false)
    private String name;

    @Column(length = 20, nullable = false)
    private String role;

    /** Тип партнёра: {@code individual} или {@code company} (null — не задан). */
    @Column(name = "partner_type", length = 20)
    private String partnerType;

    /** Контактное лицо компании (необязательно). */
    @Column(name = "contact_name")
    private String contactName;

    @Column(name = "telegram_id")
    private Long telegramId;

    @Column(name = "max_id")
    private Long maxId;

    @Column(length = 20, nullable = false)
    @Builder.Default
    private String status = STATUS_ACTIVE;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
    }
}

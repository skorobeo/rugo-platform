package ru.rugo.core.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "partner_leads")
@Getter
@Setter
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PartnerLead {

    public static final String STATUS_NEW = "new";
    public static final String STATUS_ACCEPTED = "accepted";
    public static final String STATUS_REJECTED = "rejected";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "max_user_id")
    private Long maxUserId;

    @Column(nullable = false, columnDefinition = "text")
    private String name;

    @Column(nullable = false, columnDefinition = "text")
    private String phone;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "offer_types", nullable = false)
    private List<String> offerTypes;

    /** Тип партнёра: {@code individual} или {@code company}. */
    @Column(name = "partner_type", length = 20)
    private String partnerType;

    /** Название компании (для партнёра-компании). */
    @Column(name = "company_name", columnDefinition = "text")
    private String companyName;

    /** Контактное лицо компании (необязательно). */
    @Column(name = "contact_name", columnDefinition = "text")
    private String contactName;

    @Column(columnDefinition = "text")
    private String district;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column
    private Map<String, Object> details;

    @Column(columnDefinition = "text")
    private String source;

    @Column(name = "consent_at", nullable = false)
    private OffsetDateTime consentAt;

    @Column(nullable = false, columnDefinition = "text")
    @Builder.Default
    private String status = STATUS_NEW;

    @Column(name = "notified_at")
    private OffsetDateTime notifiedAt;

    @Column(name = "decided_at")
    private OffsetDateTime decidedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    void onCreate() {
        createdAt = OffsetDateTime.now();
    }
}

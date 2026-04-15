package com.bubun.bubunapi.model;

import com.bubun.bubunapi.enums.IntegrationProvider;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "institutions", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"provider", "external_institution_id"})
})
@Getter
@NoArgsConstructor
public class Institution extends BaseEntity {

    @Column(name = "provider", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private IntegrationProvider provider;

    @Column(name = "external_institution_id", nullable = false, length = 100)
    private String externalInstitutionId;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "logo_url", nullable = false, length = 250)
    private String logoUrl;

    @Builder
    public Institution(IntegrationProvider provider, String externalInstitutionId, String name, String logoUrl) {
        this.provider = provider;
        this.externalInstitutionId = externalInstitutionId;
        this.name = name;
        this.logoUrl = logoUrl;
    }
}


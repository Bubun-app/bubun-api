package com.bubun.bubunapi.model;

import com.bubun.bubunapi.enums.IntegrationProvider;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Entity
@Table(name = "account_integrations", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"provider", "external_account_id"}),
        @UniqueConstraint(columnNames = {"provider", "account_id"})
})
@Getter
@NoArgsConstructor
public class AccountIntegration extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "institution_id", nullable = false)
    private Institution institution;

    @Column(name = "provider", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private IntegrationProvider provider;

    @Column(name = "external_account_id", nullable = false)
    private String externalAccountId;

    @Column(name = "external_requisition_id", nullable = false)
    private String externalRequisitionId;

    @Column(name = "last_synced_at", nullable = false)
    private OffsetDateTime lastSyncedAt;

    @Column(name = "sync_status", nullable = false, length = 20)
    private String syncStatus;

    @Builder
    public AccountIntegration(Account account, Institution institution, IntegrationProvider provider,
                              String externalAccountId, String externalRequisitionId, OffsetDateTime lastSyncedAt,
                              String syncStatus) {
        this.account = account;
        this.institution = institution;
        this.provider = provider;
        this.externalAccountId = externalAccountId;
        this.externalRequisitionId = externalRequisitionId;
        this.lastSyncedAt = lastSyncedAt;
        this.syncStatus = syncStatus;
    }
}

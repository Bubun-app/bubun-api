package com.bubun.bubunapi.model;

import com.bubun.bubunapi.enums.IntegrationProvider;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "account_transaction_integrations", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"provider", "external_transaction_id"}),
        @UniqueConstraint(columnNames = {"provider", "external_internal_id"})
})
@Getter
@NoArgsConstructor
public class AccountTransactionIntegration extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_transaction_id", nullable = false, unique = true)
    private AccountTransaction accountTransaction;

    @Column(name = "provider", nullable = false)
    @Enumerated(EnumType.STRING)
    private IntegrationProvider provider;

    @Column(name = "external_transaction_id", nullable = false)
    private String externalTransactionId;

    @Column(name = "external_internal_id", nullable = false)
    private String externalInternalId;

    @Builder
    public AccountTransactionIntegration(AccountTransaction accountTransaction, IntegrationProvider provider,
                                         String externalTransactionId, String externalInternalId) {
        this.accountTransaction = accountTransaction;
        this.provider = provider;
        this.externalTransactionId = externalTransactionId;
        this.externalInternalId = externalInternalId;
    }
}

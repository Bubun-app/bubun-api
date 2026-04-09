package com.bubun.bubunapi.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "account_transactions", indexes = {
        @Index(name = "idx_account_tx_account_date", columnList = "account_id,value_datetime"),
        @Index(name = "idx_account_tx_transfer", columnList = "transfer_transaction_id")
})
@Getter
@NoArgsConstructor
public class AccountTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transfer_transaction_id")
    private AccountTransaction transferTransaction;

    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "value_datetime", nullable = false)
    private OffsetDateTime valueDateTime;

    @Column(name = "booking_datetime")
    private OffsetDateTime bookingDateTime;

    @Column(name = "remittance_info")
    private String remittanceInfo;

    @Column(name = "creditor_name")
    private String creditorName;

    @Column(name = "debtor_name")
    private String debtorName;

    @Column(name = "is_manual", nullable = false)
    private Boolean isManual;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    public AccountTransaction(Account account, AccountTransaction transferTransaction, BigDecimal amount,
                              OffsetDateTime valueDateTime, OffsetDateTime bookingDateTime, String remittanceInfo,
                              String creditorName, String debtorName, Boolean isManual) {
        this.account = account;
        this.transferTransaction = transferTransaction;
        this.amount = amount;
        this.valueDateTime = valueDateTime;
        this.bookingDateTime = bookingDateTime;
        this.remittanceInfo = remittanceInfo;
        this.creditorName = creditorName;
        this.debtorName = debtorName;
        this.isManual = isManual;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = OffsetDateTime.now();
    }
}

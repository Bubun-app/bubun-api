package com.bubun.bubunapi.model;

import com.bubun.bubunapi.enums.AccountSource;
import com.bubun.bubunapi.enums.AccountType;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Currency;
import java.util.UUID;

@Entity
@Table(name = "accounts", indexes = {
        @Index(name = "idx_accounts_user", columnList = "user_id"),
        @Index(name = "idx_accounts_active", columnList = "user_id,is_active")
})
@Getter
@NoArgsConstructor
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "source", nullable = false)
    @Enumerated(EnumType.STRING)
    private AccountSource source;

    @Column(name = "type", nullable = false)
    @Enumerated(EnumType.STRING)
    private AccountType type;

    @Column(name = "personal_percentage", nullable = false, precision = 5, scale = 2)
    @DecimalMin(value = "0.00", message = "Personal percentage must be between 0 and 100")
    @DecimalMax(value = "100.00", message = "Personal percentage must be between 0 and 100")
    private BigDecimal personalPercentage;

    @Column(name = "current_balance", nullable = false, precision = 19, scale = 4)
    private BigDecimal currentBalance;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    public Account(User user, String name, Currency currency, AccountSource source, AccountType type) {
        this.user = user;
        this.name = name;
        this.currency = currency.getCurrencyCode();
        this.source = source;
        this.type = type;
        this.personalPercentage = new BigDecimal("100.00");
        this.currentBalance = BigDecimal.ZERO;
        this.isActive = true;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = OffsetDateTime.now();
    }
}



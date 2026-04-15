package com.bubun.bubunapi.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Currency;

@MappedSuperclass
@Getter
@NoArgsConstructor
public class Expense extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @Column(name = "amount", nullable = false)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "expense_timestamp_utc", nullable = false)
    private OffsetDateTime expenseTimestampUtc;

    @Column(name = "creation_timestamp_utc", nullable = false)
    private OffsetDateTime creationTimestampUtc;

    @Column(name = "description")
    private String description;

    @Column(name = "warranty_until")
    private LocalDate warrantyUntil;

    @Column(name = "receipt_url", length = 500)
    private String receiptUrl;

    @Column(name = "exchange_rate", nullable = false)
    private BigDecimal exchangeRate;

    @Column(name = "location_label")
    private String locationLabel;

    @Column(name = "latitude")
    private BigDecimal latitude;

    @Column(name = "longitude")
    private BigDecimal longitude;

    public Expense(Category category, BigDecimal amount, Currency currency, OffsetDateTime expenseTimestampUtc,
                   String description, LocalDate warrantyUntil, String receiptUrl, BigDecimal exchangeRate,
                   String locationLabel, BigDecimal latitude, BigDecimal longitude) {
        this.category = category;
        this.amount = amount;
        this.currency = currency.getCurrencyCode();
        this.expenseTimestampUtc = expenseTimestampUtc;
        this.creationTimestampUtc = OffsetDateTime.now();
        this.description = description;
        this.warrantyUntil = warrantyUntil;
        this.receiptUrl = receiptUrl;
        this.exchangeRate = exchangeRate;
        this.locationLabel = locationLabel;
        this.latitude = latitude;
        this.longitude = longitude;
    }
}

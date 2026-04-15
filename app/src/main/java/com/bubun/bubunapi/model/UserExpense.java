package com.bubun.bubunapi.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Currency;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "user_expenses", indexes = {
        @Index(name = "idx_user_expenses_user_date", columnList = "user_id,expense_timestamp_utc"),
        @Index(name = "idx_user_expenses_category", columnList = "category_id")
})
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UserExpense extends Expense {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToMany(mappedBy = "userExpenses", fetch = FetchType.LAZY)
    private Set<AccountTransaction> accountTransactions;

    @ManyToMany(mappedBy = "userExpenses", fetch = FetchType.LAZY)
    private Set<GroupExpenseSplit> groupExpenseSplits;

    @ManyToMany(mappedBy = "userExpenses", fetch = FetchType.LAZY)
    private Set<Tag> tags;

    @Builder
    public UserExpense(Category category, BigDecimal amount, Currency currency, OffsetDateTime expenseTimestampUtc,
                       String description, LocalDate warrantyUntil, String receiptUrl, BigDecimal exchangeRate,
                       String locationLabel, BigDecimal latitude, BigDecimal longitude, User user) {
        super(category, amount, currency, expenseTimestampUtc, description, warrantyUntil, receiptUrl, exchangeRate,
                locationLabel, latitude, longitude);
        this.user = user;
        this.accountTransactions = new HashSet<>();
        this.groupExpenseSplits = new HashSet<>();
        this.tags = new HashSet<>();
    }
}

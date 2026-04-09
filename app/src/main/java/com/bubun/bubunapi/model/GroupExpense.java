package com.bubun.bubunapi.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Currency;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "group_expenses", indexes = {
        @Index(name = "idx_group_expenses_group_date", columnList = "group_id,expense_timestamp_utc")
})
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class GroupExpense extends Expense {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private Group group;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id", nullable = false)
    private User creator;

    @ManyToMany(mappedBy = "groupExpenses", fetch = FetchType.LAZY)
    private Set<Tag> tags;

    public GroupExpense(Category category, BigDecimal amount, Currency currency, OffsetDateTime expenseTimestampUtc,
                       String description, OffsetDateTime warrantyUntil, String receiptUrl, BigDecimal exchangeRate,
                       String locationLabel, BigDecimal latitude, BigDecimal longitude, Group group, User creator) {
        super(category, amount, currency, expenseTimestampUtc, description, warrantyUntil, receiptUrl, exchangeRate,
                locationLabel, latitude, longitude);
        this.group = group;
        this.creator = creator;
        this.tags = new HashSet<>();
    }
}

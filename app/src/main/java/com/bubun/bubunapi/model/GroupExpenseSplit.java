package com.bubun.bubunapi.model;

import com.bubun.bubunapi.enums.SplitStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "group_expense_splits",
        uniqueConstraints = {
            @UniqueConstraint(columnNames = {"group_expense_id", "user_id"})
        }, indexes = {
            @Index(name = "idx_group_splits_user_status", columnList = "user_id,status"),
            @Index(name = "idx_group_splits_expense", columnList = "group_expense_id")
})
@Getter
@NoArgsConstructor
public class GroupExpenseSplit {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_expense_id", nullable = false)
    private GroupExpense groupExpense;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "amount_owed", nullable = false, precision = 19, scale = 4)
    @DecimalMin(value = "0.00", message = "Amount owed must be non-negative")
    private BigDecimal amountOwed;

    @Column(name = "amount_paid", nullable = false, precision = 19, scale = 4)
    @DecimalMin(value = "0.00", message = "Amount paid must be non-negative")
    private BigDecimal amountPaid;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private SplitStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    public GroupExpenseSplit(GroupExpense groupExpense, User user, BigDecimal amountOwed, BigDecimal amountPaid,
                             SplitStatus status) {
        this.groupExpense = groupExpense;
        this.user = user;
        this.amountOwed = amountOwed;
        this.amountPaid = amountPaid;
        this.status = status;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = OffsetDateTime.now();
    }
}

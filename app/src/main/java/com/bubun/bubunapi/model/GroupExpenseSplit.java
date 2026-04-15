package com.bubun.bubunapi.model;

import com.bubun.bubunapi.enums.SplitStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Set;

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
public class GroupExpenseSplit extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_expense_id", nullable = false)
    private GroupExpense groupExpense;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToMany
    @JoinTable(
            name = "user_expense_group_splits",
            joinColumns = @JoinColumn(name = "group_expense_split_id"),
            inverseJoinColumns = {@JoinColumn(
                    name = "user_expense_id",
                    referencedColumnName = "id")
            }
    )
    private Set<UserExpense> userExpenses;

    @Column(name = "amount_owed", nullable = false, precision = 19, scale = 4)
    @DecimalMin(value = "0.00", message = "Amount owed must be non-negative")
    private BigDecimal amountOwed;

    @Column(name = "amount_paid", nullable = false, precision = 19, scale = 4)
    @DecimalMin(value = "0.00", message = "Amount paid must be non-negative")
    private BigDecimal amountPaid;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private SplitStatus status;

    @Builder
    public GroupExpenseSplit(GroupExpense groupExpense, User user, BigDecimal amountOwed, BigDecimal amountPaid,
                             SplitStatus status) {
        this.groupExpense = groupExpense;
        this.user = user;
        this.amountOwed = amountOwed;
        this.amountPaid = amountPaid;
        this.status = status;
    }
}

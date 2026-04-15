package com.bubun.bubunapi.model;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "tags")
@Getter
@NoArgsConstructor
public class Tag extends BaseEntity {

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @ManyToMany
    @JoinTable(
            name = "user_expense_tags",
            joinColumns = @JoinColumn(name = "tag_id"),
            inverseJoinColumns = {@JoinColumn(
                    name = "user_expense_id",
                    referencedColumnName = "id")
            }
    )
    private Set<UserExpense> userExpenses;

    @ManyToMany
    @JoinTable(
            name = "group_expense_tags",
            joinColumns = @JoinColumn(name = "tag_id"),
            inverseJoinColumns = {@JoinColumn(
                    name = "group_expense_id",
                    referencedColumnName = "id")
            }
    )
    private Set<GroupExpense> groupExpenses;

    @Builder
    public Tag(String name) {
        this.name = name;
        this.userExpenses = new HashSet<>();
        this.groupExpenses = new HashSet<>();
    }
}

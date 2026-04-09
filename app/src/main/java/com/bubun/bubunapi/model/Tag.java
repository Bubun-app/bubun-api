package com.bubun.bubunapi.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "tags")
@Getter
@NoArgsConstructor
public class Tag {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

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
    private Set<UserExpense> groupExpenses;

    public Tag(String name) {
        this.name = name;
        this.userExpenses = new HashSet<>();
        this.groupExpenses = new HashSet<>();
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = OffsetDateTime.now();
    }
}

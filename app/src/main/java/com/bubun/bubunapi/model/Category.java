package com.bubun.bubunapi.model;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "categories", indexes = {
        @Index(name = "idx_categories_user", columnList = "user_id"),
        @Index(name = "idx_categories_parent", columnList = "parent_id")
})
@Getter
@NoArgsConstructor
public class Category extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Category parent;

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Category> children;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Builder
    public Category(User user, Category parent, String name) {
        this.user = user;
        this.name = name;
        this.parent = parent;
        this.children = new HashSet<>();
    }
}


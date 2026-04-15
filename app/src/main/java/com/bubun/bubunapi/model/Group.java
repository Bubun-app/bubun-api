package com.bubun.bubunapi.model;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Currency;

@Entity
@Table(name = "groups")
@Getter
@NoArgsConstructor
public class Group extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id", nullable = false)
    private User user;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "default_currency", nullable = false, length = 3)
    private String defaultCurrency;

    @Builder
    public Group(User user, String name, Currency currency) {
        this.user = user;
        this.name = name;
        this.defaultCurrency = currency.getCurrencyCode();
    }
}

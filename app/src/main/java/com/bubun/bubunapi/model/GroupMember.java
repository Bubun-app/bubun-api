package com.bubun.bubunapi.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "group_members", indexes = {
        @Index(name = "idx_group_members_user", columnList = "user_id"),
        @Index(name = "idx_group_members_active", columnList = "group_id,left_at")
})
@Getter
@NoArgsConstructor
public class GroupMember {

    @EmbeddedId
    private Key id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private Group group;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "balance", nullable = false, precision = 19, scale = 4)
    private BigDecimal balance;

    @Column(name = "trust_mode_enabled", nullable = false)
    private Boolean isTrustModeEnabled;

    @Column(name = "joined_at", nullable = false)
    private OffsetDateTime joinedAt;

    @Column(name = "left_at")
    private OffsetDateTime leftAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    public GroupMember(Group group, User user, Boolean trustModeEnabled) {
        this.group = group;
        this.user = user;
        this.balance = BigDecimal.ZERO;
        this.isTrustModeEnabled = trustModeEnabled;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = OffsetDateTime.now();
        this.joinedAt = OffsetDateTime.now();
    }

    @Embeddable
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class Key implements Serializable {

        @Column(name = "group_id")
        private UUID groupId;

        @Column(name = "user_id")
        private UUID userId;
    }
}

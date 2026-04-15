package com.bubun.bubunapi.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Entity
@Table(name = "group_invites")
@Getter
@NoArgsConstructor
public class GroupInvite extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private Group group;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inviter_id", nullable = false)
    private User inviter;

    @Column(name = "token", nullable = false)
    private String token;

    @Column(name = "max_uses", nullable = false)
    @Min(value = 1, message = "Max uses must be at least 1")
    private Integer maxUses;

    @Column(name = "use_count", nullable = false)
    @Min(value = 1, message = "Use count must be at least 1")
    private Integer useCount;

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    @Builder
    public GroupInvite(Group group, User inviter, String token, Integer maxUses, OffsetDateTime expiresAt) {
        this.group = group;
        this.inviter = inviter;
        this.token = token;
        this.maxUses = maxUses;
        this.useCount = 0;
        this.expiresAt = expiresAt;
    }
}

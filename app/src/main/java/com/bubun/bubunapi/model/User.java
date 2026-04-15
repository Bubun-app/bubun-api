package com.bubun.bubunapi.model;

import com.bubun.bubunapi.enums.UserMode;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;

@Entity
@Table(name = "users", indexes = {
        @Index(name = "idx_users_email_unique", columnList = "email", unique = true)
})
@Getter
@NoArgsConstructor
public class User extends BaseEntity {

    @Column(name = "username", nullable = false, unique = true, length = 50)
    private String username;

    @Column(name = "email", unique = true)
    private String email;

    @Column(name = "base_currency", nullable = false, length = 3)
    private String baseCurrency;

    @Column(name = "mode", nullable = false)
    @Enumerated(EnumType.STRING)
    private UserMode mode;

    @Column(name = "avatar_config", nullable = false, columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private JsonNode avatarConfig;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    @Builder
    public User(String username, String email, String baseCurrency, UserMode mode, JsonNode avatarConfig) {
        this.username = username;
        this.email = email;
        this.baseCurrency = baseCurrency;
        this.mode = mode;
        this.avatarConfig = avatarConfig;
    }
}


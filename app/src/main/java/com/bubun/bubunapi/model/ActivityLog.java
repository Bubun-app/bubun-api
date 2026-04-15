package com.bubun.bubunapi.model;

import com.bubun.bubunapi.enums.ActivityActionType;
import com.bubun.bubunapi.enums.ActivityEntityType;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

@Entity
@Table(name = "activity_logs", indexes = {
        @Index(name = "idx_activity_group_date", columnList = "group_id,created_at"),
        @Index(name = "idx_activity_entity", columnList = "entity_type,entity_id"),
        @Index(name = "idx_activity_user", columnList = "user_id")
})
@Getter
@NoArgsConstructor
public class ActivityLog extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private Group group;

    @Column(name = "entity_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private ActivityEntityType entityType;

    @Column(name = "entity_id", nullable = false)
    private UUID entityId;

    @Column(name = "action_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private ActivityActionType actionType;

    @Column(name = "metadata", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private JsonNode metadata;

    @Column(name = "old_data", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private JsonNode oldData;

    @Column(name = "new_data", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private JsonNode newData;

    @Builder
    public ActivityLog(User user, Group group, ActivityEntityType entityType, UUID entityId,
                       ActivityActionType actionType, JsonNode metadata, JsonNode oldData, JsonNode newData) {
        this.user = user;
        this.group = group;
        this.entityType = entityType;
        this.entityId = entityId;
        this.actionType = actionType;
        this.metadata = metadata;
        this.oldData = oldData;
        this.newData = newData;
    }
}

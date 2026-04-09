package com.bubun.bubunapi.model;

import com.bubun.bubunapi.enums.ContactStatus;
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
@Table(name = "contacts", indexes = {
        @Index(name = "idx_contacts_sender", columnList = "sender_user_id"),
        @Index(name = "idx_contacts_receiver", columnList = "receiver_user_id")
})
@Getter
@NoArgsConstructor
public class Contact {

    @EmbeddedId
    private Key id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("senderUserId")
    @JoinColumn(name = "sender_user_id", nullable = false)
    private User senderUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("receiverUserId")
    @JoinColumn(name = "receiver_user_id", nullable = false)
    private User receiverUser;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private ContactStatus status;

    @Column(name = "balance_from_sender", nullable = false, precision = 19, scale = 4)
    private BigDecimal balanceFromSender;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    public Contact(User senderUser, User receiverUser, ContactStatus status, BigDecimal balanceFromSender) {
        this.senderUser = senderUser;
        this.receiverUser = receiverUser;
        this.id = new Key(senderUser.getId(), receiverUser.getId());
        this.status = status;
        this.balanceFromSender = balanceFromSender;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = OffsetDateTime.now();
    }

    @Embeddable
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class Key implements Serializable {

        @Column(name = "sender_user_id")
        private UUID senderUserId;

        @Column(name = "receiver_user_id")
        private UUID receiverUserId;
    }
}




package com.bubun.bubunapi.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "settlements", indexes = {
        @Index(name = "idx_settlements_group", columnList = "group_id"),
        @Index(name = "idx_settlements_sender", columnList = "sender_user_id"),
        @Index(name = "idx_settlements_receiver", columnList = "receiver_user_id")
})
@Getter
@NoArgsConstructor
public class Settlement {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private Group group;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_user_id", nullable = false)
    private User sender;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_user_id", nullable = false)
    private User receiver;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_account_transaction_id", nullable = false)
    private AccountTransaction senderAccountTransaction;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_account_transaction_id", nullable = false)
    private AccountTransaction receiverAccountTransaction;

    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    @DecimalMin(value = "0.00", message = "Settlement amount must be non-negative")
    private BigDecimal amount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    public Settlement(Group group, User sender, User receiver, AccountTransaction senderAccountTransaction,
                      AccountTransaction receiverAccountTransaction, BigDecimal amount) {
        this.group = group;
        this.sender = sender;
        this.receiver = receiver;
        this.senderAccountTransaction = senderAccountTransaction;
        this.receiverAccountTransaction = receiverAccountTransaction;
        this.amount = amount;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = OffsetDateTime.now();
    }
}

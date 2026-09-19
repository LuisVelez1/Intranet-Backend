package com.backendintranet.entity;

import jakarta.persistence.*;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/** Durable outbox: created atomically with the request or decision. */
@Entity
@Table(
        name = "absence_mail_outbox",
        indexes = @Index(name = "idx_absence_mail_pending", columnList = "sent_at,next_attempt_at"))
@Getter
@Setter
@NoArgsConstructor
public class AbsenceMail {
    @Id
    @Column(length = 36)
    private String id;

    @Column(nullable = false, length = 150)
    private String recipient;

    @Column(nullable = false, length = 200)
    private String subject;

    @Column(nullable = false, length = 2000)
    private String body;

    @Column(nullable = false)
    private int attempts;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "next_attempt_at", nullable = false)
    private LocalDateTime nextAttemptAt;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "last_error", length = 100)
    private String lastError;

    @PrePersist
    void create() {
        id = UUID.randomUUID().toString();
        createdAt = LocalDateTime.now();
        nextAttemptAt = createdAt;
    }
}

package com.backendintranet.entity;

import jakarta.persistence.*;

import lombok.*;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.*;
import java.util.UUID;

@Entity
@Table(
        name = "absence_requests",
        indexes = {
            @Index(name = "idx_absence_requester_created", columnList = "requester_id,created_at"),
            @Index(name = "idx_absence_approver_status", columnList = "approver_id,status")
        })
@Getter
@Setter
@NoArgsConstructor
public class AbsenceRequest {
    @Id
    @Column(length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requester_id", nullable = false)
    private User requester;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "approver_id", nullable = false)
    private User approver;

    @Column(nullable = false, length = 100)
    private String type;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "start_time")
    private LocalTime startTime;

    @Column(name = "end_time")
    private LocalTime endTime;

    @Column(nullable = false, length = 2000)
    private String reason;

    @Column(name = "support_file", length = 500)
    private String supportFile;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private AbsenceStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "approval_comment", length = 2000)
    private String approvalComment;

    @Column(name = "boss_decision", length = 20)
    private String bossDecision;

    @Column(name = "boss_decision_at")
    private LocalDateTime bossDecisionAt;

    @Column(name = "boss_comment", length = 2000)
    private String bossComment;

    @Column(name = "hr_decision", length = 20)
    private String hrDecision;

    @Column(name = "hr_decision_at")
    private LocalDateTime hrDecisionAt;

    @Column(name = "hr_comment", length = 2000)
    private String hrComment;

    @PrePersist
    void create() {
        if (id == null) id = UUID.randomUUID().toString();
        createdAt = updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    void update() {
        updatedAt = LocalDateTime.now();
    }
}

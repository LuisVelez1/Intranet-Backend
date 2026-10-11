package com.backendintranet.dto.response;

import com.backendintranet.entity.AbsenceStatus;

import lombok.*;

import java.time.*;

@Data
@Builder
public class AbsenceResponse {
    private String id;
    private String requesterId;
    private String requesterName;
    private String approverId;
    private String approverName;
    private String type;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private String reason;
    private String supportFile;
    private AbsenceStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime approvedAt;
    private String approvalComment;

    private String bossDecision;
    private LocalDateTime bossDecisionAt;
    private String bossComment;

    private String hrDecision;
    private LocalDateTime hrDecisionAt;
    private String hrComment;
}

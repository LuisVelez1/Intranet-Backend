package com.backendintranet.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AbsenceApproverAssignmentRequest {

    @NotBlank
    private String approverId;
}

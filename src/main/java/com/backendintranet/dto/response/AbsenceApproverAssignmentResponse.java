package com.backendintranet.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AbsenceApproverAssignmentResponse {

    private Long id;

    private String employeeId;
    private String employeeName;
    private String employeeEmail;

    private String approverId;
    private String approverName;
    private String approverEmail;

    private Boolean active;
}

package com.backendintranet.service;

import com.backendintranet.dto.request.AbsenceApproverAssignmentRequest;
import com.backendintranet.dto.response.AbsenceApproverAssignmentResponse;
import com.backendintranet.dto.response.AbsenceApproverUserResponse;

import java.util.List;

public interface AbsenceApproverAssignmentService {

    List<AbsenceApproverAssignmentResponse> findAll();

    List<AbsenceApproverUserResponse> findActiveUsers();

    AbsenceApproverAssignmentResponse assign(
            String employeeId,
            AbsenceApproverAssignmentRequest request);

    AbsenceApproverAssignmentResponse deactivate(
            String employeeId);
}

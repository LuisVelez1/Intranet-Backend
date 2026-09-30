package com.backendintranet.service;

import com.backendintranet.dto.request.AbsenceCreateRequest;
import com.backendintranet.dto.request.AbsenceDecisionRequest;
import com.backendintranet.dto.response.AbsenceResponse;

import java.util.List;
import java.util.Map;

public interface AbsenceService {

    AbsenceResponse create(
            AbsenceCreateRequest request);

    List<AbsenceResponse> findMyRequests();

    List<AbsenceResponse> findPendingApprovals();

    List<AbsenceResponse> findAllForReporting();

    Map<String, Boolean> getCapabilities();

    AbsenceResponse approve(
            String id,
            AbsenceDecisionRequest request);

    AbsenceResponse reject(
            String id,
            AbsenceDecisionRequest request);
}

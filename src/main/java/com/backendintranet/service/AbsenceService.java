package com.backendintranet.service;

import com.backendintranet.dto.request.*;
import com.backendintranet.dto.response.AbsenceResponse;

import java.util.List;

public interface AbsenceService {
    AbsenceResponse create(AbsenceCreateRequest request);

    List<AbsenceResponse> findMyRequests();

    List<AbsenceResponse> findPendingApprovals();

    AbsenceResponse approve(String id, AbsenceDecisionRequest request);

    AbsenceResponse reject(String id, AbsenceDecisionRequest request);
}

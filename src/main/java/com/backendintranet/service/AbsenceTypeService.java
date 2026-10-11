package com.backendintranet.service;

import com.backendintranet.dto.request.AbsenceTypeRequest;
import com.backendintranet.dto.response.AbsenceTypeResponse;

import java.util.List;

public interface AbsenceTypeService {

    List<AbsenceTypeResponse> findActive();

    List<AbsenceTypeResponse> findAll();

    AbsenceTypeResponse create(
            AbsenceTypeRequest request);

    AbsenceTypeResponse update(
            Long id,
            AbsenceTypeRequest request);

    AbsenceTypeResponse deactivate(
            Long id);

    AbsenceTypeResponse activate(
            Long id);
}

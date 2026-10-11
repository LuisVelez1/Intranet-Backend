package com.backendintranet.service.impl;

import com.backendintranet.dto.request.AbsenceTypeRequest;
import com.backendintranet.dto.response.AbsenceTypeResponse;
import com.backendintranet.entity.AbsenceType;
import com.backendintranet.exception.BadRequestException;
import com.backendintranet.exception.ResourceNotFoundException;
import com.backendintranet.repository.AbsenceTypeRepository;
import com.backendintranet.service.AbsenceTypeService;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class AbsenceTypeServiceImpl
        implements AbsenceTypeService {

    private final AbsenceTypeRepository repository;

    @Override
    @Transactional(readOnly = true)
    public List<AbsenceTypeResponse> findActive() {

        return repository
                .findByActiveTrueOrderByNameAsc()
                .stream()
                .map(this::response)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AbsenceTypeResponse> findAll() {

        return repository
                .findAllByOrderByNameAsc()
                .stream()
                .map(this::response)
                .toList();
    }

    @Override
    public AbsenceTypeResponse create(
            AbsenceTypeRequest request) {

        String name =
                normalizeName(
                        request.getName());

        if (repository.existsByNameIgnoreCase(name)) {

            throw new BadRequestException(
                    "Ya existe un tipo de ausencia con ese nombre");
        }

        AbsenceType type =
                new AbsenceType();

        type.setName(name);

        type.setDescription(
                normalizeDescription(
                        request.getDescription()));

        type.setActive(
                request.getActive() == null
                        ? true
                        : request.getActive());

        return response(
                repository.saveAndFlush(type));
    }

    @Override
    public AbsenceTypeResponse update(
            Long id,
            AbsenceTypeRequest request) {

        AbsenceType type =
                repository.findById(id)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Tipo de ausencia no encontrado"));

        String name =
                normalizeName(
                        request.getName());

        repository
                .findByNameIgnoreCase(name)
                .filter(existing ->
                        !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new BadRequestException(
                            "Ya existe un tipo de ausencia con ese nombre");
                });

        type.setName(name);

        type.setDescription(
                normalizeDescription(
                        request.getDescription()));

        if (request.getActive() != null) {
            type.setActive(
                    request.getActive());
        }

        return response(
                repository.saveAndFlush(type));
    }

    @Override
    public AbsenceTypeResponse deactivate(
            Long id) {

        AbsenceType type =
                repository.findById(id)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Tipo de ausencia no encontrado"));

        type.setActive(false);

        return response(
                repository.saveAndFlush(type));
    }

    @Override
    public AbsenceTypeResponse activate(
            Long id) {

        AbsenceType type =
                repository.findById(id)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Tipo de ausencia no encontrado"));

        type.setActive(true);

        return response(
                repository.saveAndFlush(type));
    }

    private String normalizeName(
            String value) {

        if (value == null
                || value.trim().isEmpty()) {

            throw new BadRequestException(
                    "El nombre del tipo de ausencia es obligatorio");
        }

        return value.trim();
    }

    private String normalizeDescription(
            String value) {

        if (value == null) {
            return null;
        }

        String normalized =
                value.trim();

        return normalized.isEmpty()
                ? null
                : normalized;
    }

    private AbsenceTypeResponse response(
            AbsenceType type) {

        return AbsenceTypeResponse
                .builder()
                .id(type.getId())
                .name(type.getName())
                .description(
                        type.getDescription())
                .active(type.getActive())
                .build();
    }
}

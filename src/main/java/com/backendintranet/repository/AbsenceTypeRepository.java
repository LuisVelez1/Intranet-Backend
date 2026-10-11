package com.backendintranet.repository;

import com.backendintranet.entity.AbsenceType;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AbsenceTypeRepository
        extends JpaRepository<AbsenceType, Long> {

    List<AbsenceType> findByActiveTrueOrderByNameAsc();

    List<AbsenceType> findAllByOrderByNameAsc();

    Optional<AbsenceType> findByNameIgnoreCase(
            String name);

    Optional<AbsenceType> findByNameIgnoreCaseAndActiveTrue(
            String name);

    boolean existsByNameIgnoreCase(
            String name);
}

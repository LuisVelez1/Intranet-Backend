package com.backendintranet.repository;

import com.backendintranet.entity.AbsenceApproverAssignment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AbsenceApproverAssignmentRepository
        extends JpaRepository<AbsenceApproverAssignment, Long> {

    Optional<AbsenceApproverAssignment>
            findByEmployee_IdAndActiveTrue(String employeeId);

    Optional<AbsenceApproverAssignment>
            findByEmployee_Id(String employeeId);

    boolean existsByApprover_IdAndActiveTrue(String approverId);
}

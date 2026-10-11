package com.backendintranet.repository;

import com.backendintranet.entity.AbsenceRequest;
import com.backendintranet.entity.AbsenceStatus;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AbsenceRequestRepository
        extends JpaRepository<AbsenceRequest, String> {

    List<AbsenceRequest> findByRequester_IdOrderByCreatedAtDesc(
            String requesterId);

    List<AbsenceRequest> findByApprover_IdAndStatusOrderByCreatedAtAsc(
            String approverId,
            AbsenceStatus status);

    // Talento Humano / Super Admin:
    // todas las solicitudes pendientes.
    List<AbsenceRequest> findByStatusOrderByCreatedAtAsc(
            AbsenceStatus status);

    // Indicadores corporativos.
    List<AbsenceRequest> findAllByOrderByCreatedAtDesc();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from AbsenceRequest a where a.id = :id")
    Optional<AbsenceRequest> findForDecision(
            @Param("id") String id);
}

package com.backendintranet.repository;

import com.backendintranet.entity.AbsenceMail;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.*;

import java.time.LocalDateTime;
import java.util.List;

public interface AbsenceMailRepository extends JpaRepository<AbsenceMail, String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<AbsenceMail> findTop20BySentAtIsNullAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
            LocalDateTime now);
}

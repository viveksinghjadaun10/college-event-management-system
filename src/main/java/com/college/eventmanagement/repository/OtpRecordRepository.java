package com.college.eventmanagement.repository;

import com.college.eventmanagement.model.OtpRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface OtpRecordRepository extends JpaRepository<OtpRecord, Long> {

    Optional<OtpRecord> findFirstByMobileNumberAndUsedFalseOrderByCreatedAtDesc(String mobileNumber);

    long countByMobileNumberAndCreatedAtAfter(String mobileNumber, LocalDateTime time);
}
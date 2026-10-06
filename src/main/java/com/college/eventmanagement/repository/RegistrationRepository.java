package com.college.eventmanagement.repository;

import com.college.eventmanagement.model.Registration;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RegistrationRepository extends JpaRepository<Registration, Long> {
    boolean existsByEventIdAndStudentId(Long eventId, Long studentId);
    long countByEventId(Long eventId);
    List<Registration> findByEventId(Long eventId);
    List<Registration> findByStudentId(Long studentId);
    Optional<Registration> findByTicketCode(String ticketCode);
}
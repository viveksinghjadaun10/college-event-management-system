package com.college.eventmanagement.repository;

import com.college.eventmanagement.model.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;

public interface EventRepository extends JpaRepository<Event, Long> {
    List<Event> findByCategoryIgnoreCase(String category);
    List<Event> findByEventDateBetween(LocalDate start, LocalDate end);
}
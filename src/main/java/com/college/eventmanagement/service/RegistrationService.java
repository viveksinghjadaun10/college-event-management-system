package com.college.eventmanagement.service;

import com.college.eventmanagement.exception.BadRequestException;
import com.college.eventmanagement.exception.ResourceNotFoundException;
import com.college.eventmanagement.model.Event;
import com.college.eventmanagement.model.Registration;
import com.college.eventmanagement.model.Student;
import com.college.eventmanagement.repository.EventRepository;
import com.college.eventmanagement.repository.RegistrationRepository;
import com.college.eventmanagement.repository.StudentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class RegistrationService {

    private final RegistrationRepository registrationRepository;
    private final EventRepository eventRepository;
    private final StudentRepository studentRepository;

    public RegistrationService(RegistrationRepository registrationRepository,
                               EventRepository eventRepository,
                               StudentRepository studentRepository) {
        this.registrationRepository = registrationRepository;
        this.eventRepository = eventRepository;
        this.studentRepository = studentRepository;
    }

    @Transactional
    public Registration register(Long eventId, Long studentId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with id " + eventId));
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with id " + studentId));

        if (event.getEventDate().isBefore(LocalDate.now())) {
            throw new BadRequestException("Registration closed: this event has already taken place");
        }
        if (registrationRepository.existsByEventIdAndStudentId(eventId, studentId)) {
            throw new BadRequestException("Student is already registered for this event");
        }
        if (registrationRepository.countByEventId(eventId) >= event.getTotalSeats()) {
            throw new BadRequestException("No seats available for this event");
        }

        Registration registration = new Registration();
        registration.setEvent(event);
        registration.setStudent(student);
        registration.setRegisteredAt(LocalDateTime.now());
        registration.setTicketCode(UUID.randomUUID().toString());
        return registrationRepository.save(registration);
    }

    public List<Registration> getParticipants(Long eventId) {
        if (!eventRepository.existsById(eventId)) {
            throw new ResourceNotFoundException("Event not found with id " + eventId);
        }
        return registrationRepository.findByEventId(eventId);
    }

    public List<Registration> getRegistrationsOfStudent(Long studentId) {
        if (!studentRepository.existsById(studentId)) {
            throw new ResourceNotFoundException("Student not found with id " + studentId);
        }
        return registrationRepository.findByStudentId(studentId);
    }

    public void cancelRegistration(Long registrationId) {
        Registration registration = registrationRepository.findById(registrationId)
                .orElseThrow(() -> new ResourceNotFoundException("Registration not found with id " + registrationId));
        registrationRepository.delete(registration);
    }
}
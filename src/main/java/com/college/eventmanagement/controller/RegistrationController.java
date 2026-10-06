package com.college.eventmanagement.controller;

import com.college.eventmanagement.dto.RegistrationRequest;
import com.college.eventmanagement.model.Registration;
import com.college.eventmanagement.service.RegistrationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/registrations")
public class RegistrationController {

    private final RegistrationService registrationService;

    public RegistrationController(RegistrationService registrationService) {
        this.registrationService = registrationService;
    }

    @PostMapping
    public ResponseEntity<Registration> register(@Valid @RequestBody RegistrationRequest request) {
        Registration registration = registrationService.register(request.eventId(), request.studentId());
        return ResponseEntity.status(HttpStatus.CREATED).body(registration);
    }

    @GetMapping("/event/{eventId}")
    public List<Registration> participants(@PathVariable Long eventId) {
        return registrationService.getParticipants(eventId);
    }

    @GetMapping("/student/{studentId}")
    public List<Registration> byStudent(@PathVariable Long studentId) {
        return registrationService.getRegistrationsOfStudent(studentId);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancel(@PathVariable Long id) {
        registrationService.cancelRegistration(id);
        return ResponseEntity.noContent().build();
    }
}
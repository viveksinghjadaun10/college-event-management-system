package com.college.eventmanagement.dto;

import jakarta.validation.constraints.NotNull;

public record RegistrationRequest(
        @NotNull(message = "eventId is required") Long eventId,
        @NotNull(message = "studentId is required") Long studentId) {
}
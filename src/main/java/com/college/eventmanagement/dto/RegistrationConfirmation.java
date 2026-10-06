package com.college.eventmanagement.dto;

import java.time.LocalDate;

public record RegistrationConfirmation(
        Long registrationId,
        String ticketCode,
        String message,
        String eventName,
        LocalDate eventDate,
        String venue) {
}
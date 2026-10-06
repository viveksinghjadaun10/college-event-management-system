package com.college.eventmanagement.service;

import com.college.eventmanagement.dto.RegistrationConfirmation;
import com.college.eventmanagement.dto.SendOtpRequest;
import com.college.eventmanagement.dto.VerifyOtpRequest;
import com.college.eventmanagement.exception.BadRequestException;
import com.college.eventmanagement.model.Event;
import com.college.eventmanagement.model.Registration;
import com.college.eventmanagement.model.Student;
import com.college.eventmanagement.repository.RegistrationRepository;
import com.college.eventmanagement.repository.StudentRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Optional;

@Service
public class OtpRegistrationService {

    private static final DateTimeFormatter SMS_DATE =
            DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH);

    private final OtpService otpService;
    private final EventService eventService;
    private final StudentService studentService;
    private final RegistrationService registrationService;
    private final StudentRepository studentRepository;
    private final RegistrationRepository registrationRepository;
    private final SmsService smsService;

    public OtpRegistrationService(OtpService otpService,
                                  EventService eventService,
                                  StudentService studentService,
                                  RegistrationService registrationService,
                                  StudentRepository studentRepository,
                                  RegistrationRepository registrationRepository,
                                  SmsService smsService) {
        this.otpService = otpService;
        this.eventService = eventService;
        this.studentService = studentService;
        this.registrationService = registrationService;
        this.studentRepository = studentRepository;
        this.registrationRepository = registrationRepository;
        this.smsService = smsService;
    }

    public void sendOtp(SendOtpRequest request) {
        Event event = eventService.getEventById(request.eventId());
        ensureRegistrationOpen(event);

        studentRepository.findByMobileNumber(request.mobileNumber()).ifPresent(student -> {
            if (registrationRepository.existsByEventIdAndStudentId(event.getId(), student.getId())) {
                throw new BadRequestException("This mobile number is already registered for this event");
            }
        });

        otpService.sendOtp(request.mobileNumber());
    }

    // No @Transactional here on purpose: a wrong OTP attempt must stay saved
    public RegistrationConfirmation verifyAndRegister(VerifyOtpRequest request) {
        Event event = eventService.getEventById(request.eventId());
        ensureRegistrationOpen(event);

        Optional<Student> existing = studentRepository.findByMobileNumber(request.mobileNumber());
        if (existing.isEmpty() && studentRepository.existsByEmail(request.email())) {
            throw new BadRequestException("This email is already used by another student");
        }

        otpService.verifyOtp(request.mobileNumber(), request.otp());

        Student student = existing.orElseGet(() -> {
            Student newStudent = new Student();
            newStudent.setName(request.name());
            newStudent.setEmail(request.email());
            newStudent.setDepartment(request.department());
            newStudent.setMobileNumber(request.mobileNumber());
            return studentService.createStudent(newStudent);
        });

        Registration registration = registrationService.register(event.getId(), student.getId());

        String date = event.getEventDate().format(SMS_DATE).toUpperCase(Locale.ENGLISH);
        String message = "Registered for " + event.getName() + " on " + date
                + " at " + event.getVenue() + ". Registration ID: " + registration.getId() + ".";
        smsService.sendSms(request.mobileNumber(), message);

        return new RegistrationConfirmation(registration.getId(), registration.getTicketCode(), message,
                event.getName(), event.getEventDate(), event.getVenue());
    }

    private void ensureRegistrationOpen(Event event) {
        if (event.getEventDate().isBefore(LocalDate.now())) {
            throw new BadRequestException("Registration closed: this event has already taken place");
        }
        if (registrationRepository.countByEventId(event.getId()) >= event.getTotalSeats()) {
            throw new BadRequestException("No seats available for this event");
        }
    }
}
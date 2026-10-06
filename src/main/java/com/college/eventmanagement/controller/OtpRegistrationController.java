package com.college.eventmanagement.controller;

import com.college.eventmanagement.dto.RegistrationConfirmation;
import com.college.eventmanagement.dto.SendOtpRequest;
import com.college.eventmanagement.dto.VerifyOtpRequest;
import com.college.eventmanagement.service.OtpRegistrationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/register")
public class OtpRegistrationController {

    private final OtpRegistrationService otpRegistrationService;

    public OtpRegistrationController(OtpRegistrationService otpRegistrationService) {
        this.otpRegistrationService = otpRegistrationService;
    }

    @PostMapping("/send-otp")
    public ResponseEntity<Map<String, String>> sendOtp(@Valid @RequestBody SendOtpRequest request) {
        otpRegistrationService.sendOtp(request);
        String mobile = request.mobileNumber();
        String masked = "XXXXXX" + mobile.substring(mobile.length() - 4);
        return ResponseEntity.ok(Map.of("message", "OTP sent to " + masked));
    }

    @PostMapping("/verify")
    public ResponseEntity<RegistrationConfirmation> verify(@Valid @RequestBody VerifyOtpRequest request) {
        RegistrationConfirmation confirmation = otpRegistrationService.verifyAndRegister(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(confirmation);
    }
}
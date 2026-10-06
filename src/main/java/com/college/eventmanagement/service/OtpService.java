package com.college.eventmanagement.service;

import com.college.eventmanagement.exception.BadRequestException;
import com.college.eventmanagement.model.OtpRecord;
import com.college.eventmanagement.repository.OtpRecordRepository;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HexFormat;

@Service
public class OtpService {

    private static final int OTP_VALID_MINUTES = 5;
    private static final int MAX_WRONG_ATTEMPTS = 3;
    private static final int MAX_REQUESTS = 3;
    private static final int REQUEST_WINDOW_MINUTES = 10;

    private final OtpRecordRepository otpRecordRepository;
    private final SmsService smsService;
    private final SecureRandom secureRandom = new SecureRandom();

    public OtpService(OtpRecordRepository otpRecordRepository, SmsService smsService) {
        this.otpRecordRepository = otpRecordRepository;
        this.smsService = smsService;
    }

    public void sendOtp(String mobileNumber) {
        LocalDateTime now = LocalDateTime.now();

        long recentRequests = otpRecordRepository.countByMobileNumberAndCreatedAtAfter(
                mobileNumber, now.minusMinutes(REQUEST_WINDOW_MINUTES));
        if (recentRequests >= MAX_REQUESTS) {
            throw new BadRequestException("Too many OTP requests. Try again after some time");
        }

        String otp = String.format("%06d", secureRandom.nextInt(1_000_000));

        OtpRecord record = new OtpRecord();
        record.setMobileNumber(mobileNumber);
        record.setOtpHash(hash(mobileNumber, otp));
        record.setCreatedAt(now);
        record.setExpiresAt(now.plusMinutes(OTP_VALID_MINUTES));
        record.setAttempts(0);
        record.setUsed(false);
        otpRecordRepository.save(record);

        smsService.sendSms(mobileNumber,
                otp + " is your OTP for college event registration. It is valid for "
                        + OTP_VALID_MINUTES + " minutes. Do not share it with anyone.");
    }

    public void verifyOtp(String mobileNumber, String otp) {
        OtpRecord record = otpRecordRepository
                .findFirstByMobileNumberAndUsedFalseOrderByCreatedAtDesc(mobileNumber)
                .orElseThrow(() -> new BadRequestException("No OTP found for this number. Request a new OTP"));

        if (record.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("OTP has expired. Request a new OTP");
        }
        if (record.getAttempts() >= MAX_WRONG_ATTEMPTS) {
            throw new BadRequestException("Too many wrong attempts. Request a new OTP");
        }

        boolean matches = MessageDigest.isEqual(
                hash(mobileNumber, otp).getBytes(StandardCharsets.UTF_8),
                record.getOtpHash().getBytes(StandardCharsets.UTF_8));

        if (!matches) {
            record.setAttempts(record.getAttempts() + 1);
            otpRecordRepository.save(record);
            throw new BadRequestException("Incorrect OTP");
        }

        record.setUsed(true);
        otpRecordRepository.save(record);
    }

    private String hash(String mobileNumber, String otp) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest((mobileNumber + ":" + otp).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }
}
package com.college.eventmanagement.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ConsoleSmsService implements SmsService {

    private static final Logger log = LoggerFactory.getLogger(ConsoleSmsService.class);

    @Override
    public void sendSms(String mobileNumber, String message) {
        log.info("SMS to {} : {}", mobileNumber, message);
    }
}
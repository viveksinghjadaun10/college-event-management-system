package com.college.eventmanagement.service;

public interface SmsService {
    void sendSms(String mobileNumber, String message);
}
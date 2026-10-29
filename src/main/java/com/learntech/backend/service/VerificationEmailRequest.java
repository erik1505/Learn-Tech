package com.learntech.backend.service;

public record VerificationEmailRequest(String recipient, String subject, String body) {}

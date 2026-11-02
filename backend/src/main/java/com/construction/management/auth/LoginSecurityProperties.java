package com.construction.management.auth;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.auth.login")
public record LoginSecurityProperties(
        @Min(1) int maxFailedAttempts,
        @Min(1) int lockMinutes) {
}

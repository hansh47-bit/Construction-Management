package com.construction.management.notification;

import jakarta.mail.internet.InternetAddress;
import java.net.URI;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.approval-email")
public record ApprovalEmailProperties(boolean enabled, String from, String baseUrl, int maxAttempts) {
    public ApprovalEmailProperties {
        from = from == null ? "" : from.trim();
        baseUrl = baseUrl == null ? "" : baseUrl.trim().replaceAll("/+$", "");
        if (maxAttempts < 1 || maxAttempts > 10) {
            throw new IllegalArgumentException("메일 재시도 횟수는 1~10회로 설정하세요.");
        }
        if (enabled) {
            if (!validAddress(from)) throw new IllegalArgumentException("알림 발신 Gmail 주소를 설정하세요.");
            try {
                URI uri = URI.create(baseUrl);
                if (!("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                        || uri.getHost() == null || uri.getUserInfo() != null || uri.getQuery() != null || uri.getFragment() != null) {
                    throw new IllegalArgumentException();
                }
            } catch (IllegalArgumentException exception) {
                throw new IllegalArgumentException("직원들이 접속하는 프로그램 주소(APP_BASE_URL)를 설정하세요.");
            }
        }
    }

    static boolean validAddress(String value) {
        if (value == null || value.isBlank() || value.indexOf('\r') >= 0 || value.indexOf('\n') >= 0) return false;
        try {
            InternetAddress address = new InternetAddress(value.trim(), true);
            address.validate();
            return value.trim().equals(address.getAddress()) && address.getAddress().contains("@");
        } catch (jakarta.mail.internet.AddressException exception) {
            return false;
        }
    }
}

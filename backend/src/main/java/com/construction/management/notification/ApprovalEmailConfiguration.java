package com.construction.management.notification;

import org.springframework.boot.autoconfigure.mail.MailProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({ApprovalEmailProperties.class, MailProperties.class})
public class ApprovalEmailConfiguration {
    public ApprovalEmailConfiguration(ApprovalEmailProperties properties, MailProperties mail) {
        if (properties.enabled() && (!StringUtils.hasText(mail.getUsername()) || !StringUtils.hasText(mail.getPassword()))) {
            throw new IllegalArgumentException("Gmail 주소와 앱 비밀번호를 설정한 뒤 메일 알림을 켜세요.");
        }
    }
}

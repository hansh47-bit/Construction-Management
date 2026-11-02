package com.construction.management.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.support.ResourcePropertySource;

class LoginSecurityPropertiesTest {
    private final ApplicationContextRunner runner = new ApplicationContextRunner()
        .withUserConfiguration(PropertiesConfiguration.class)
        .withInitializer(context -> {
            try {
                context.getEnvironment().getPropertySources().addLast(
                    new ResourcePropertySource("classpath:application.properties"));
            } catch (IOException exception) {
                throw new UncheckedIOException(exception);
            }
        });

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(LoginSecurityProperties.class)
    static class PropertiesConfiguration {}

    @Test void defaultsRemainFiveAttemptsAndFifteenMinutes() {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context.getBean(LoginSecurityProperties.class))
                .isEqualTo(new LoginSecurityProperties(5, 15));
        });
    }

    @Test void environmentNamesOverrideBothDefaults() {
        runner.withPropertyValues("LOGIN_MAX_FAILED_ATTEMPTS=9", "LOGIN_LOCK_MINUTES=27")
            .run(context -> {
                assertThat(context).hasNotFailed();
                assertThat(context.getBean(LoginSecurityProperties.class))
                    .isEqualTo(new LoginSecurityProperties(9, 27));
            });
    }

    @Test void dotenvFileIsLoadedAsConfiguration(@TempDir Path directory) throws IOException {
        Path file = directory.resolve(".env");
        Files.writeString(file, "LOGIN_MAX_FAILED_ATTEMPTS=7\nLOGIN_LOCK_MINUTES=25\n");
        new ApplicationContextRunner()
            .withUserConfiguration(PropertiesConfiguration.class)
            .withPropertyValues("spring.config.import=optional:" + file.toUri() + "[.properties]")
            .withInitializer(new ConfigDataApplicationContextInitializer())
            .run(context -> {
                assertThat(context).hasNotFailed();
                assertThat(context.getBean(LoginSecurityProperties.class))
                    .isEqualTo(new LoginSecurityProperties(7, 25));
            });
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "not-a-number"})
    void invalidAttemptLimitPreventsStartup(String value) {
        runner.withPropertyValues("LOGIN_MAX_FAILED_ATTEMPTS=" + value)
            .run(context -> assertThat(context).hasFailed());
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "not-a-number"})
    void invalidLockDurationPreventsStartup(String value) {
        runner.withPropertyValues("LOGIN_LOCK_MINUTES=" + value)
            .run(context -> assertThat(context).hasFailed());
    }
}

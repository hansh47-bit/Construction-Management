package com.construction.management.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.construction.management.auth.ApiException;
import com.construction.management.auth.LoginService;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:configured-login-lock;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
    "app.demo-data.enabled=false", "LOGIN_MAX_FAILED_ATTEMPTS=3", "LOGIN_LOCK_MINUTES=2"
})
class ConfiguredLoginLockIntegrationTest {
    private static final String PASSWORD = "Configured-password123!";
    @Autowired LoginService login;
    @Autowired AppUserRepository users;
    @Autowired PasswordEncoder encoder;

    AppUser user() {
        AppUser user = new AppUser("configured-" + UUID.randomUUID().toString().substring(0, 8),
            encoder.encode(PASSWORD), "Configured login test", AccountType.USER, Position.STAFF, "test");
        user.mustChangePassword = false;
        return users.saveAndFlush(user);
    }

    void fail(AppUser user, String password) {
        assertThatThrownBy(() -> login.authenticate(user.loginId, password, user.getId()))
            .isInstanceOfSatisfying(ApiException.class, error -> {
                assertThat(error.getStatus()).isEqualTo(401);
                assertThat(error.getCode()).isEqualTo("LOGIN_FAILED");
            });
    }

    AppUser reload(AppUser user) { return users.findById(user.getId()).orElseThrow(); }

    @Test void customThresholdAndDurationAreAppliedAndLockedAttemptsDoNotExtendLock() {
        AppUser user = user();
        for (int attempt = 1; attempt < 3; attempt++) {
            fail(user, "incorrect-password");
            assertThat(reload(user).failedLoginCount).isEqualTo(attempt);
            assertThat(reload(user).locked()).isFalse();
        }
        LocalDateTime before = LocalDateTime.now();
        fail(user, "incorrect-password");
        LocalDateTime after = LocalDateTime.now();
        AppUser locked = reload(user);
        assertThat(locked.failedLoginCount).isEqualTo(3);
        assertThat(locked.lockedUntil).isBetween(before.plusMinutes(2), after.plusMinutes(2));
        assertThat(locked.locked()).isTrue();

        fail(user, PASSWORD);
        AppUser stillLocked = reload(user);
        assertThat(stillLocked.failedLoginCount).isEqualTo(3);
        assertThat(stillLocked.lockedUntil).isEqualTo(locked.lockedUntil);

        stillLocked.lockedUntil = LocalDateTime.now().minusSeconds(1);
        users.saveAndFlush(stillLocked);
        login.authenticate(user.loginId, PASSWORD, user.getId());
        assertThat(reload(user).failedLoginCount).isZero();
        assertThat(reload(user).lockedUntil).isNull();
    }

    @Test void successfulLoginResetsFailuresBeforeCustomThreshold() {
        AppUser user = user();
        fail(user, "incorrect-password");
        fail(user, "incorrect-password");
        login.authenticate(user.loginId, PASSWORD, user.getId());
        AppUser loggedIn = reload(user);
        assertThat(loggedIn.failedLoginCount).isZero();
        assertThat(loggedIn.lockedUntil).isNull();
        assertThat(encoder.matches(PASSWORD, loggedIn.getPasswordHash())).isTrue();
    }
}

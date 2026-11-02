package com.construction.management.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:admin-bootstrap-test;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
    "app.demo-data.enabled=false"
})
class AdminBootstrapTest {
    @Autowired AdminBootstrap bootstrap;
    @Autowired AppUserRepository users;
    @Autowired SecurityAuditLogRepository audit;
    @Autowired PasswordEncoder encoder;

    @BeforeEach
    void prepareExistingAccount() {
        users.deleteAllInBatch();
        audit.deleteAllInBatch();
        users.saveAndFlush(new AppUser("previous-admin", encoder.encode("Previous!Password123"),
                "기존 관리자", AccountType.ADMIN, null, "test"));
    }

    @Test
    void normalInitializationDoesNotOverwriteExistingAccounts() {
        assertThatThrownBy(() -> bootstrap.create("inc-admin", "남기성", "Replacement!Password123"))
                .isInstanceOf(IllegalStateException.class);
        assertThat(users.findByLoginId("previous-admin")).isPresent();
        assertThat(users.count()).isEqualTo(1);
    }

    @Test
    void explicitResetReplacesAccountsWithTemporaryAdministratorAndKeepsAuditHistory() {
        bootstrap.resetAndCreate("inc-admin", "남기성", "Replacement!Password123");

        assertThat(users.count()).isEqualTo(1);
        assertThat(users.findByLoginId("previous-admin")).isEmpty();
        AppUser admin = users.findByLoginId("inc-admin").orElseThrow();
        assertThat(admin.getAccountType()).isEqualTo(AccountType.ADMIN);
        assertThat(admin.isMustChangePassword()).isTrue();
        assertThat(encoder.matches("Replacement!Password123", admin.getPasswordHash())).isTrue();
        assertThat(audit.findAll()).extracting(SecurityAuditLog::getAction)
                .contains("ACCOUNTS_RESET", "ADMIN_BOOTSTRAP");
    }

    @Test
    void invalidReplacementPasswordLeavesExistingAccountIntact() {
        assertThatThrownBy(() -> bootstrap.resetAndCreate("inc-admin", "남기성", "short"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(users.findByLoginId("previous-admin")).isPresent();
        assertThat(users.count()).isEqualTo(1);
        assertThat(audit.count()).isZero();
    }
}

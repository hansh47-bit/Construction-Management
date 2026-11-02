package com.construction.management.account;

import static org.assertj.core.api.Assertions.assertThat;

import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

@SpringBootTest(properties = {"app.demo-data.enabled=false"})
@Import(PermissionSchemaMigrationIntegrationTest.LegacyDatabase.class)
class PermissionSchemaMigrationIntegrationTest {
    @Autowired PositionPermissionRepository permissions;

    @Test
    void startupWidensExistingPermissionEnumAndPreservesCustomizedPermissions() {
        for (Position position : Position.values()) {
            assertThat(permissions.findByPosition(position)).hasSize(Permission.values().length);
        }
        PositionPermission review = permissions.findById("TEAM_LEADER:APPROVAL_REVIEW").orElseThrow();
        assertThat(review.getPermissionKey()).isEqualTo(Permission.APPROVAL_REVIEW);
        assertThat(review.isAllowed()).isTrue();
        PositionPermission customized = permissions.findById("STAFF:SITE_WRITE").orElseThrow();
        assertThat(customized.isAllowed()).isFalse();
        assertThat(customized.getVersion()).isEqualTo(7);
    }

    @TestConfiguration
    static class LegacyDatabase {
        @Bean
        DataSource dataSource() throws Exception {
            DriverManagerDataSource source = new DriverManagerDataSource(
                    "jdbc:h2:mem:permission-enum-upgrade-test;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1", "sa", "");
            try (var connection = source.getConnection(); var statement = connection.createStatement()) {
                statement.execute("""
                        CREATE TABLE position_permissions (
                            id VARCHAR(255) PRIMARY KEY,
                            position ENUM('CEO','FINANCE','STAFF','TEAM_LEADER') NOT NULL,
                            permission_key ENUM('APPROVAL_READ','PROJECT_READ','SITE_READ','SITE_WRITE') NOT NULL,
                            allowed BOOLEAN NOT NULL,
                            updated_at TIMESTAMP,
                            updated_by VARCHAR(255),
                            version BIGINT NOT NULL,
                            UNIQUE(position,permission_key)
                        )
                        """);
                statement.execute("""
                        INSERT INTO position_permissions(id,position,permission_key,allowed,version,updated_by)
                        VALUES ('STAFF:SITE_WRITE','STAFF','SITE_WRITE',FALSE,7,'legacy-admin')
                        """);
            }
            return source;
        }
    }
}

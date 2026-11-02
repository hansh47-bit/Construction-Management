package com.construction.management.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:legacy-permission-test;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
    "spring.h2.console.enabled=false", "app.demo-data.enabled=false"
})
@AutoConfigureMockMvc
class LegacyPermissionIntegrationTest {
    private static final String PASSWORD = "Temporary-pass123!";
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired AppUserRepository users;
    @Autowired PasswordEncoder encoder;
    @Autowired JdbcTemplate jdbc;

    private JsonNode body(MvcResult result) throws Exception {
        return json.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    @Test void retiredKeysDoNotBreakCreatedAccountLoginOrPermissionManagement() throws Exception {
        // Emulate an older database whose permission column admits retired values.
        // This DDL and fixture affect only this test's dedicated in-memory DB.
        jdbc.execute("ALTER TABLE position_permissions ALTER COLUMN permission_key VARCHAR(255)");
        for (Position position : Position.values()) {
            jdbc.update("INSERT INTO position_permissions (id, position, permission_key, allowed, version) VALUES (?, ?, ?, ?, 0)",
                position + ":APPROVAL_REVIEW", position.name(), "APPROVAL_REVIEW", true);
        }
        AppUser admin = new AppUser("legacy-admin", encoder.encode(PASSWORD), "Legacy admin", AccountType.ADMIN, null, "test");
        admin.mustChangePassword = false;
        users.saveAndFlush(admin);
        var adminLogin = mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(Map.of("login_id", admin.getLoginId(), "password", PASSWORD))))
            .andExpect(status().isOk()).andReturn();
        MockHttpSession adminSession = (MockHttpSession) adminLogin.getRequest().getSession(false);
        mvc.perform(post("/api/v1/admin/users").session(adminSession).contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(Map.of("login_id", "legacy-staff", "name", "New staff", "position", "STAFF", "temporary_password", PASSWORD))))
            .andExpect(status().isCreated());

        var login = mvc.perform(post("/api/v1/auth/login").session(adminSession).contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(Map.of("login_id", "legacy-staff", "password", PASSWORD))))
            .andExpect(status().isOk()).andExpect(jsonPath("must_change_password").value(true))
            .andExpect(jsonPath("permissions", org.hamcrest.Matchers.hasItem("PROJECT_READ")))
            .andExpect(jsonPath("permissions", org.hamcrest.Matchers.not(org.hamcrest.Matchers.hasItem("APPROVAL_REVIEW"))))
            .andReturn();
        assertThat(adminSession.isInvalid()).isTrue();
        MockHttpSession staffSession = (MockHttpSession) login.getRequest().getSession(false);
        mvc.perform(get("/api/v1/auth/me").session(staffSession)).andExpect(status().isOk());

        adminLogin = mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(Map.of("login_id", admin.getLoginId(), "password", PASSWORD))))
            .andExpect(status().isOk()).andReturn();
        adminSession = (MockHttpSession) adminLogin.getRequest().getSession(false);
        JsonNode positions = body(mvc.perform(get("/api/v1/admin/positions").session(adminSession))
            .andExpect(status().isOk()).andReturn());
        long version = -1;
        for (JsonNode position : positions) {
            for (JsonNode permission : position.path("permissions")) {
                assertThat(permission.path("key").asText()).isNotEqualTo("APPROVAL_REVIEW");
                if (position.path("position").asText().equals("STAFF") && permission.path("key").asText().equals("SITE_WRITE")) {
                    version = permission.path("version").asLong();
                }
            }
        }
        assertThat(version).isGreaterThanOrEqualTo(0);
        mvc.perform(put("/api/v1/admin/positions/STAFF/permissions").session(adminSession).contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(Map.of("permissions", Map.of("SITE_WRITE", false), "versions", Map.of("SITE_WRITE", version), "reason", "Legacy compatibility check"))))
            .andExpect(status().isOk());
        mvc.perform(get("/api/v1/auth/me").session(staffSession)).andExpect(status().isOk())
            .andExpect(jsonPath("permissions", org.hamcrest.Matchers.not(org.hamcrest.Matchers.hasItem("SITE_WRITE"))));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM position_permissions WHERE permission_key = ? AND allowed = TRUE",
            Integer.class, "APPROVAL_REVIEW")).isEqualTo(Position.values().length);
    }
}

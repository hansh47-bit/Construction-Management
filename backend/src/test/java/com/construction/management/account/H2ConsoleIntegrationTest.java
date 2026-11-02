package com.construction.management.account;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
    "spring.datasource.url=jdbc:h2:mem:h2-console-test;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
    "spring.h2.console.enabled=true", "server.servlet.session.cookie.secure=false", "app.demo-data.enabled=false"
})
@ActiveProfiles("prod")
class H2ConsoleIntegrationTest {
    private static final String PASSWORD = "Console-test-password123!";
    @LocalServerPort int port;
    @Autowired AppUserRepository users;
    @Autowired PasswordEncoder encoder;
    @Autowired ObjectMapper json;

    HttpClient client() {
        return HttpClient.newBuilder().cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL))
            .followRedirects(HttpClient.Redirect.NORMAL).build();
    }

    URI uri(String path) { return URI.create("http://127.0.0.1:" + port + path); }

    HttpResponse<String> get(HttpClient client, String path) throws Exception {
        return client.send(HttpRequest.newBuilder(uri(path)).GET().build(), HttpResponse.BodyHandlers.ofString());
    }

    HttpClient login(AccountType type, boolean temporary) throws Exception {
        AppUser user = new AppUser("console-" + UUID.randomUUID().toString().substring(0, 8),
            encoder.encode(PASSWORD), "Console test", type, type == AccountType.ADMIN ? null : Position.CEO, "test");
        user.mustChangePassword = temporary;
        users.saveAndFlush(user);
        HttpClient client = client();
        var request = HttpRequest.newBuilder(uri("/api/v1/auth/login"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(Map.of("login_id", user.loginId, "password", PASSWORD)))).build();
        assertThat(client.send(request, HttpResponse.BodyHandlers.ofString()).statusCode()).isEqualTo(200);
        return client;
    }

    @Test void anonymousConsoleAccessShowsDatabaseLogin() throws Exception {
        var response = get(client(), "/h2-console/");
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("H2");
        assertThat(get(client(), "/api/v1/projects").statusCode()).isEqualTo(401);
    }

    @Test void databaseLoginDoesNotRequireAppLoginAndAppApiStillRequiresAuthentication() throws Exception {
        HttpClient client = client();
        var page = get(client, "/h2-console/");
        assertThat(page.statusCode()).isEqualTo(200);
        assertThat(page.headers().firstValue("X-Frame-Options")).contains("SAMEORIGIN");
        assertThat(page.body()).contains("H2");
        var session = java.util.regex.Pattern.compile("jsessionid=([a-zA-Z0-9]+)").matcher(page.body());
        assertThat(session.find()).as("H2 console session token").isTrue();
        String sessionId = session.group(1);
        String url = "jdbc:h2:mem:h2-console-test;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;IFEXISTS=TRUE";
        String form = "driver=org.h2.Driver&url=" + URLEncoder.encode(url, StandardCharsets.UTF_8) + "&user=sa&password=";
        var request = HttpRequest.newBuilder(uri("/h2-console/login.do?jsessionid=" + sessionId))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString(form)).build();
        var connected = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertThat(connected.statusCode()).isEqualTo(200);
        assertThat(connected.body()).contains("frame");
        request = HttpRequest.newBuilder(uri("/h2-console/query.do?jsessionid=" + sessionId))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString("sql=" + URLEncoder.encode("SELECT COUNT(*) FROM app_users", StandardCharsets.UTF_8))).build();
        var query = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertThat(query.statusCode()).isEqualTo(200);
        assertThat(query.body()).containsIgnoringCase("count(*)").doesNotContain("Table &quot;app_users&quot; not found");
        request = HttpRequest.newBuilder(uri("/api/v1/auth/logout")).POST(HttpRequest.BodyPublishers.noBody()).build();
        var loggedOut = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertThat(loggedOut.statusCode()).isEqualTo(200);
        assertThat(get(client, "/api/v1/projects").statusCode()).isEqualTo(401);
    }

    @Test void consoleUsesDatabaseCredentialsRegardlessOfAppAccountType() throws Exception {
        var response = get(login(AccountType.USER, false), "/h2-console/");
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("H2");
    }

    @Test void consoleDoesNotApplyAppPasswordChangeRequirement() throws Exception {
        HttpClient client = login(AccountType.ADMIN, true);
        var response = get(client, "/h2-console/");
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("H2");
        response = get(client, "/api/v1/projects");
        assertThat(response.statusCode()).isEqualTo(403);
        assertThat(response.body()).contains("PASSWORD_CHANGE_REQUIRED");
    }
}

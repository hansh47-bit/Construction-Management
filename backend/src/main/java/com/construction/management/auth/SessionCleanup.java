package com.construction.management.auth;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Component;

@Component
public class SessionCleanup {
    private final ServerProperties properties;

    public SessionCleanup(ServerProperties properties) {
        this.properties = properties;
    }

    public void clearSession(HttpServletRequest request, HttpServletResponse response) {
        new SecurityContextLogoutHandler().logout(request, response,
            SecurityContextHolder.getContext().getAuthentication());
    }

    public void expireCookie(HttpServletRequest request, HttpServletResponse response) {
        var settings = properties.getServlet().getSession().getCookie();
        String name = settings.getName() == null ? "JSESSIONID" : settings.getName();
        String path = settings.getPath();
        if (path == null || path.isBlank()) {
            path = request.getContextPath().isEmpty() ? "/" : request.getContextPath();
        }
        Cookie cookie = new Cookie(name, "");
        cookie.setPath(path);
        if (settings.getDomain() != null && !settings.getDomain().isBlank()) {
            cookie.setDomain(settings.getDomain());
        }
        cookie.setHttpOnly(true);
        cookie.setMaxAge(0);
        cookie.setSecure(settings.getSecure() == null ? request.isSecure() : settings.getSecure());
        response.addCookie(cookie);
    }

    public void clear(HttpServletRequest request, HttpServletResponse response) {
        clearSession(request, response);
        expireCookie(request, response);
    }
}

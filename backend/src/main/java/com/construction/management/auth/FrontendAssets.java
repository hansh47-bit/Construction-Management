package com.construction.management.auth;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Set;

/** Only the built login application is public; uploaded files and APIs are not. */
public final class FrontendAssets {
 private static final Set<String> FILES = Set.of("/", "/index.html", "/favicon.ico", "/vite.svg");
 private FrontendAssets() {}

 public static boolean isPublicRead(HttpServletRequest request) {
  if (!Set.of("GET", "HEAD").contains(request.getMethod())) return false;
  String path = request.getRequestURI().substring(request.getContextPath().length());
  return FILES.contains(path) || path.startsWith("/assets/");
 }
}

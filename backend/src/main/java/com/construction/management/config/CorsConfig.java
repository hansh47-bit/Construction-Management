package com.construction.management.config;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.web.cors.*;
@Configuration
public class CorsConfig {
 @Bean CorsConfigurationSource corsConfigurationSource(@Value("${app.cors.allowed-origins:http://localhost:5173,http://127.0.0.1:5173}") String origins) {
  var config=new CorsConfiguration(); var allowed=Arrays.stream(origins.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
  if(allowed.stream().anyMatch(s -> s.contains("*"))) throw new IllegalArgumentException("CORS Origin에는 와일드카드를 사용할 수 없습니다.");
  config.setAllowedOrigins(allowed); config.setAllowedMethods(List.of("GET","POST","PUT","PATCH","DELETE","OPTIONS")); config.setAllowedHeaders(List.of("Content-Type","X-CSRF-TOKEN","X-XSRF-TOKEN")); config.setAllowCredentials(true); config.setExposedHeaders(List.of("X-Request-ID"));
  var source=new UrlBasedCorsConfigurationSource(); source.registerCorsConfiguration("/api/**",config); return source;
 }
}

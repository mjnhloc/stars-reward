package com.sr.config;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

  @Bean
  SecurityFilterChain securityFilterChain(
    HttpSecurity http,
    UrlBasedCorsConfigurationSource corsConfigurationSource
  ) throws Exception {
    http.csrf(csrf -> csrf.disable())
      .cors(cors -> cors.configurationSource(corsConfigurationSource))
      .authorizeHttpRequests(auth -> auth
        .requestMatchers(HttpMethod.OPTIONS, "/**")
        .permitAll()
        .requestMatchers("/actuator/health/**", "/api/**")
        .permitAll()
        .anyRequest()
        .authenticated());
    return http.build();
  }

  // @Bean
  // @ConditionalOnProperty(name = "app.security.enabled", havingValue = "false",
  // matchIfMissing = true)
  // SecurityFilterChain local(HttpSecurity http) throws Exception {
  // return http.csrf(c -> c.disable()).cors(c -> {
  // }).authorizeHttpRequests(a -> a.anyRequest().permitAll()).build();
  // }

  @Bean
  UrlBasedCorsConfigurationSource corsConfigurationSource(@Value("${app.cors.allowed-origins}") List<String> origins) {
    var c = new CorsConfiguration();
    c.setAllowedOrigins(origins);

    c.setAllowedMethods(List.of(
      "GET",
      "POST",
      "PUT",
      "PATCH",
      "DELETE",
      "OPTIONS"));

    c.setAllowedHeaders(List.of(
      "Authorization",
      "Content-Type",
      "Idempotency-Key"));

    c.setExposedHeaders(List.of(
      "Location"));

    var source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", c);

    return source;
  }
}

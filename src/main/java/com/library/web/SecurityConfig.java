package com.library.web;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration configuration)
            throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.authorizeRequests(auth -> auth
                        .antMatchers("/", "/index.html", "/app.js", "/style.css", "/favicon.ico",
                                "/api/health", "/api/auth/login", "/api/auth/register", "/api/auth/csrf",
                                "/error").permitAll()
                        .antMatchers(org.springframework.http.HttpMethod.GET, "/api/books/**").permitAll()
                        .antMatchers("/api/moderation/**").hasRole("MODERATOR")
                        .antMatchers("/api/books/**", "/api/auth/logout").authenticated()
                        .anyRequest().permitAll())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .exceptionHandling(handlers -> handlers.authenticationEntryPoint(
                        (request, response, exception) -> response.sendError(401, "Требуется авторизация")))
                .logout().disable();
        return http.build();
    }
}

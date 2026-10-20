package com.aadi.sandesh.config;

import com.aadi.sandesh.dto.common.ApiError;
import com.aadi.sandesh.security.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

@Configuration
public class SecurityConfig {
    private final List<String> allowedOrigins;
    private final Environment environment;

    public SecurityConfig(
            @Value("${sandesh.websocket.allowed-origins:http://localhost:4173,http://localhost:4200}")
            String allowedOrigins,
            Environment environment) {
        this.allowedOrigins = Arrays.asList(allowedOrigins.split(","));
        this.environment = environment;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    /**
     * Prevents Spring Boot from auto-generating an in-memory user and default password on startup.
     */
    @Bean
    public UserDetailsService userDetailsService() {
        return username -> {
            throw new UsernameNotFoundException("Stateless JWT authentication only");
        };
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthenticationFilter jwtAuthenticationFilter,
            ObjectMapper objectMapper
    ) throws Exception {
        boolean isDevOrTest = environment.matchesProfiles("dev", "test");

        return http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            var apiError = new ApiError(
                                    Instant.now(),
                                    HttpServletResponse.SC_UNAUTHORIZED,
                                    "UNAUTHORIZED",
                                    "Authentication required",
                                    request.getRequestURI(),
                                    List.of()
                            );
                            objectMapper.writeValue(response.getOutputStream(), apiError);
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            var apiError = new ApiError(
                                    Instant.now(),
                                    HttpServletResponse.SC_FORBIDDEN,
                                    "FORBIDDEN",
                                    accessDeniedException.getMessage() != null
                                            ? accessDeniedException.getMessage()
                                            : "Access denied",
                                    request.getRequestURI(),
                                    List.of()
                            );
                            objectMapper.writeValue(response.getOutputStream(), apiError);
                        })
                )
                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers("/api/auth/**", "/ws/**").permitAll()
                            .requestMatchers("/", "/index.html", "/demo.css", "/demo.js", "/favicon.ico").permitAll()
                            .requestMatchers("/actuator/health/**", "/actuator/info/**").permitAll()
                            .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll();

                    if (isDevOrTest) {
                        auth.requestMatchers("/test/**").permitAll();
                    }

                    auth.anyRequest().authenticated();
                })
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        var configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(allowedOrigins);
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Cache-Control", "Content-Type", "X-Request-Id"));
        configuration.setExposedHeaders(Arrays.asList("Authorization", "X-Request-Id"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}

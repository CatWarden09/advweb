package ru.catwarden.advweb.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final KeycloakJwtRoleConverter keycloakJwtRoleConverter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/error").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .requestMatchers(HttpMethod.GET, "/uploads/**").permitAll()

                        .requestMatchers(HttpMethod.GET, "/advertisements/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/advertisements/search").permitAll()

                        .requestMatchers("/users/*/reviews/received").permitAll()
                        .requestMatchers("/users/*/advertisements").permitAll()

                        .requestMatchers(HttpMethod.GET, "/categories/**").permitAll()

                        .requestMatchers("/moderation/**").hasRole("ADMIN")

                        .requestMatchers("/reviews/**").hasAnyRole("USER", "ADMIN")

                        .requestMatchers("/images/**").hasAnyRole("USER", "ADMIN")

                        .requestMatchers("/comments/**").hasAnyRole("USER", "ADMIN")

                        .requestMatchers("/avatars/**").hasAnyRole("USER", "ADMIN")

                        .requestMatchers(HttpMethod.POST, "/advertisements/**").hasAnyRole("USER", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/advertisements/**").hasAnyRole("USER", "ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/advertisements/**").hasAnyRole("USER", "ADMIN")

                        .requestMatchers(HttpMethod.POST, "/categories/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/categories/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/categories/**").hasRole("ADMIN")

                        .requestMatchers("/admin/**").hasRole("ADMIN")

                        .requestMatchers("/users/**").hasAnyRole("USER", "ADMIN")

                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(keycloakJwtRoleConverter))
                );
        return http.build();
    }
}

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
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()

                        .requestMatchers(HttpMethod.GET, "/advertisements/**").permitAll()

                        .requestMatchers("/users/*/reviews/received").permitAll()
                        .requestMatchers("/users/*/advertisements").permitAll()


                        .requestMatchers("/moderation/**").hasRole("ADMIN")

                        .requestMatchers("/reviews/**").hasRole("USER")

                        .requestMatchers("/images/**").hasRole("USER")

                        .requestMatchers("/comments/**").hasRole("USER")

                        .requestMatchers("/avatars/**").hasRole("USER")

                        .requestMatchers(HttpMethod.POST, "/advertisements/**").hasRole("USER")
                        .requestMatchers(HttpMethod.PATCH, "/advertisements/**").hasRole("USER")
                        .requestMatchers(HttpMethod.DELETE, "/advertisements/**").hasRole("USER")

                        .requestMatchers("/admin/**").hasRole("ADMIN")

                        .requestMatchers("/users/**").hasRole("USER")


                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(keycloakJwtRoleConverter))
                );
        return http.build();
    }
}

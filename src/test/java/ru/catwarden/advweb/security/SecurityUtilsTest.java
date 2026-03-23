package ru.catwarden.advweb.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SecurityUtilsTest {

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getCurrentJwtReturnsJwtWhenPrincipalIsJwt() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject("user-1")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .claim("scope", "read")
                .build();
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new TestingAuthenticationToken(jwt, null));
        SecurityContextHolder.setContext(context);

        Jwt result = SecurityUtils.getCurrentJwt();

        assertEquals(jwt, result);
    }

    @Test
    void getCurrentJwtReturnsNullWhenNoAuthentication() {
        SecurityContextHolder.clearContext();

        Jwt result = SecurityUtils.getCurrentJwt();

        assertNull(result);
    }

    @Test
    void getCurrentJwtReturnsNullWhenPrincipalIsNotJwt() {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new TestingAuthenticationToken("regular-user", null));
        SecurityContextHolder.setContext(context);

        Jwt result = SecurityUtils.getCurrentJwt();

        assertNull(result);
    }

    @Test
    void getCurrentUserKeycloakIdReturnsSubjectWhenJwtExists() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject("kc-123")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new TestingAuthenticationToken(jwt, null));
        SecurityContextHolder.setContext(context);

        String result = SecurityUtils.getCurrentUserKeycloakId();

        assertEquals("kc-123", result);
    }

    @Test
    void getCurrentUserKeycloakIdReturnsNullWhenJwtMissing() {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new TestingAuthenticationToken("user", null));
        SecurityContextHolder.setContext(context);

        String result = SecurityUtils.getCurrentUserKeycloakId();

        assertNull(result);
    }

    @Test
    void isAuthenticatedReturnsFalseWhenAuthenticationIsNull() {
        SecurityContextHolder.clearContext();

        boolean result = SecurityUtils.isAuthenticated();

        assertFalse(result);
    }

    @Test
    void isAuthenticatedReturnsFalseWhenAuthenticationIsNotAuthenticated() {
        TestingAuthenticationToken authentication = new TestingAuthenticationToken("user", null);
        authentication.setAuthenticated(false);
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);

        boolean result = SecurityUtils.isAuthenticated();

        assertFalse(result);
    }

    @Test
    void isAuthenticatedReturnsFalseForAnonymousUser() {
        TestingAuthenticationToken authentication = new TestingAuthenticationToken("anonymousUser", null, "ROLE_USER");
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);

        boolean result = SecurityUtils.isAuthenticated();

        assertFalse(result);
    }

    @Test
    void isAuthenticatedReturnsTrueForRegularAuthenticatedUser() {
        TestingAuthenticationToken authentication = new TestingAuthenticationToken("john", null, "ROLE_USER");
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);

        boolean result = SecurityUtils.isAuthenticated();

        assertTrue(result);
    }

    @Test
    void hasRoleReturnsFalseWhenAuthenticationIsNull() {
        SecurityContextHolder.clearContext();

        boolean result = SecurityUtils.hasRole("ADMIN");

        assertFalse(result);
    }

    @Test
    void hasRoleSupportsRoleNameWithoutPrefix() {
        TestingAuthenticationToken authentication = new TestingAuthenticationToken(
                "john",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"), new SimpleGrantedAuthority("ROLE_USER"))
        );
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);

        boolean result = SecurityUtils.hasRole("ADMIN");

        assertTrue(result);
    }

    @Test
    void hasRoleSupportsRoleNameWithPrefix() {
        TestingAuthenticationToken authentication = new TestingAuthenticationToken(
                "john",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);

        boolean result = SecurityUtils.hasRole("ROLE_ADMIN");

        assertTrue(result);
    }

    @Test
    void hasRoleReturnsFalseWhenRoleMissing() {
        TestingAuthenticationToken authentication = new TestingAuthenticationToken(
                "john",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);

        boolean result = SecurityUtils.hasRole("ADMIN");

        assertFalse(result);
    }

    @Test
    void isCurrentUserAdminReturnsTrueWhenAdminRolePresent() {
        TestingAuthenticationToken authentication = new TestingAuthenticationToken(
                "john",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);

        boolean result = SecurityUtils.isCurrentUserAdmin();

        assertTrue(result);
    }

    @Test
    void isCurrentUserAdminReturnsFalseWhenAdminRoleMissing() {
        TestingAuthenticationToken authentication = new TestingAuthenticationToken(
                "john",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);

        boolean result = SecurityUtils.isCurrentUserAdmin();

        assertFalse(result);
    }
}

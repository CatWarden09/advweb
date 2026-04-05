package ru.catwarden.advweb.security;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.security.config.annotation.web.configurers.oauth2.server.resource.OAuth2ResourceServerConfigurer;
import org.springframework.security.web.DefaultSecurityFilterChain;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.http.HttpMethod;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SecurityConfigTest {

    @Test
    void securityFilterChainReturnsBuiltChain() throws Exception {
        KeycloakJwtRoleConverter converter = mock(KeycloakJwtRoleConverter.class);
        SecurityConfig securityConfig = new SecurityConfig(converter);

        HttpSecurity http = mock(HttpSecurity.class);
        DefaultSecurityFilterChain expectedChain = mock(DefaultSecurityFilterChain.class);
        when(http.cors(any())).thenReturn(http);
        when(http.csrf(any())).thenReturn(http);
        when(http.authorizeHttpRequests(any())).thenReturn(http);
        when(http.oauth2ResourceServer(any())).thenReturn(http);
        when(http.build()).thenReturn(expectedChain);

        SecurityFilterChain result = securityConfig.securityFilterChain(http);

        assertEquals(expectedChain, result);
        verify(http).cors(any());
        verify(http).csrf(any());
        verify(http).authorizeHttpRequests(any());
        verify(http).oauth2ResourceServer(any());
        verify(http).build();
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void securityFilterChainDisablesCsrfAndUsesKeycloakConverter() throws Exception {
        KeycloakJwtRoleConverter converter = mock(KeycloakJwtRoleConverter.class);
        SecurityConfig securityConfig = new SecurityConfig(converter);

        HttpSecurity http = mock(HttpSecurity.class);
        DefaultSecurityFilterChain builtChain = mock(DefaultSecurityFilterChain.class);
        when(http.cors(any())).thenReturn(http);
        when(http.csrf(any())).thenReturn(http);
        when(http.authorizeHttpRequests(any())).thenReturn(http);
        when(http.oauth2ResourceServer(any())).thenReturn(http);
        when(http.build()).thenReturn(builtChain);

        securityConfig.securityFilterChain(http);

        ArgumentCaptor<Customizer> csrfCaptor = ArgumentCaptor.forClass(Customizer.class);
        verify(http).csrf(csrfCaptor.capture());
        CsrfConfigurer csrfConfigurer = mock(CsrfConfigurer.class);
        csrfCaptor.getValue().customize(csrfConfigurer);
        verify(csrfConfigurer).disable();

        ArgumentCaptor<Customizer> oauth2Captor = ArgumentCaptor.forClass(Customizer.class);
        verify(http).oauth2ResourceServer(oauth2Captor.capture());

        OAuth2ResourceServerConfigurer oauth2Configurer = mock(OAuth2ResourceServerConfigurer.class);
        OAuth2ResourceServerConfigurer.JwtConfigurer jwtConfigurer = mock(OAuth2ResourceServerConfigurer.JwtConfigurer.class);

        doAnswer(invocation -> {
            Customizer<OAuth2ResourceServerConfigurer.JwtConfigurer> jwtCustomizer = invocation.getArgument(0);
            jwtCustomizer.customize(jwtConfigurer);
            return oauth2Configurer;
        }).when(oauth2Configurer).jwt(any());

        oauth2Captor.getValue().customize(oauth2Configurer);

        verify(jwtConfigurer).jwtAuthenticationConverter(converter);
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void securityFilterChainConfiguresAuthorizationRules() throws Exception {
        KeycloakJwtRoleConverter converter = mock(KeycloakJwtRoleConverter.class);
        SecurityConfig securityConfig = new SecurityConfig(converter);

        HttpSecurity http = mock(HttpSecurity.class);
        when(http.cors(any())).thenReturn(http);
        when(http.csrf(any())).thenReturn(http);
        when(http.authorizeHttpRequests(any())).thenReturn(http);
        when(http.oauth2ResourceServer(any())).thenReturn(http);
        when(http.build()).thenReturn(mock(DefaultSecurityFilterChain.class));

        securityConfig.securityFilterChain(http);

        ArgumentCaptor<Customizer> authorizeCaptor = ArgumentCaptor.forClass(Customizer.class);
        verify(http).authorizeHttpRequests(authorizeCaptor.capture());

        AuthorizeHttpRequestsConfigurer.AuthorizationManagerRequestMatcherRegistry registry =
                mock(AuthorizeHttpRequestsConfigurer.AuthorizationManagerRequestMatcherRegistry.class);
        AuthorizeHttpRequestsConfigurer.AuthorizedUrl authorizedUrl =
                mock(AuthorizeHttpRequestsConfigurer.AuthorizedUrl.class);

        when(registry.requestMatchers(any(String[].class))).thenReturn(authorizedUrl);
        when(registry.requestMatchers(any(HttpMethod.class), any(String[].class))).thenReturn(authorizedUrl);
        when(registry.anyRequest()).thenReturn(authorizedUrl);

        when(authorizedUrl.permitAll()).thenReturn(registry);
        when(authorizedUrl.hasRole(anyString())).thenReturn(registry);
        when(authorizedUrl.hasAnyRole(any(String[].class))).thenReturn(registry);
        when(authorizedUrl.authenticated()).thenReturn(registry);

        authorizeCaptor.getValue().customize(registry);

        verify(registry).requestMatchers(HttpMethod.OPTIONS, "/**");
        verify(registry).requestMatchers("/error");
        verify(registry).requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html");
        verify(registry).requestMatchers(HttpMethod.GET, "/uploads/**");
        verify(registry).requestMatchers("/admin/**");
        verify(registry).anyRequest();

        verify(authorizedUrl, atLeastOnce()).permitAll();
        verify(authorizedUrl, atLeastOnce()).hasRole("ADMIN");
        verify(authorizedUrl, atLeastOnce()).hasAnyRole("USER", "ADMIN");
        verify(authorizedUrl).authenticated();
    }
}

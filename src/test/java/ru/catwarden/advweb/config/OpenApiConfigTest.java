package ru.catwarden.advweb.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class OpenApiConfigTest {

    @Test
    void customOpenApiBuildsExpectedInfoAndOAuth2Scheme() {
        OpenApiConfig config = new OpenApiConfig();
        String issuerUri = "http://localhost:8080/realms/advweb";
        ReflectionTestUtils.setField(config, "issuerUri", issuerUri);

        OpenAPI openApi = config.customOpenAPI();

        assertNotNull(openApi);
        assertNotNull(openApi.getInfo());
        assertEquals("Advweb API", openApi.getInfo().getTitle());
        assertEquals("0.3", openApi.getInfo().getVersion());
        assertEquals("Advertisements website API with Keycloak integration", openApi.getInfo().getDescription());

        assertNotNull(openApi.getSecurity());
        assertEquals(1, openApi.getSecurity().size());
        assertNotNull(openApi.getSecurity().getFirst().get("oauth2"));

        assertNotNull(openApi.getComponents());
        SecurityScheme oauth2Scheme = openApi.getComponents().getSecuritySchemes().get("oauth2");
        assertNotNull(oauth2Scheme);
        assertEquals(SecurityScheme.Type.OAUTH2, oauth2Scheme.getType());
        assertNotNull(oauth2Scheme.getFlows());
        assertNotNull(oauth2Scheme.getFlows().getAuthorizationCode());
        assertEquals(
                issuerUri + "/protocol/openid-connect/auth",
                oauth2Scheme.getFlows().getAuthorizationCode().getAuthorizationUrl()
        );
        assertEquals(
                issuerUri + "/protocol/openid-connect/token",
                oauth2Scheme.getFlows().getAuthorizationCode().getTokenUrl()
        );
    }
}

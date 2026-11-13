package io.devforge.platform.core.security;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles({"test", "wso2"})
class Wso2SecurityConfigurationTests {

    @LocalServerPort
    private int port;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    private RestClient http;

    @BeforeEach
    void setUp() {
        http = RestClient.builder().baseUrl("http://localhost:" + port).build();
        when(jwtDecoder.decode("read-token")).thenReturn(jwt("read-token", "devforge.read"));
        when(jwtDecoder.decode("write-token")).thenReturn(jwt("write-token", "devforge.write"));
    }

    @Test
    void rejectsAnonymousApiRequests() {
        RestClientResponseException response = assertThrows(RestClientResponseException.class,
                () -> http.get().uri("/api/v1/projects").retrieve().toBodilessEntity());
        assertEquals(401, response.getStatusCode().value());
    }

    @Test
    void rejectsAuthenticatedRequestsWithoutReadScope() {
        RestClientResponseException response = assertThrows(RestClientResponseException.class,
                () -> http.get().uri("/api/v1/projects")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer write-token")
                        .retrieve().toBodilessEntity());
        assertEquals(403, response.getStatusCode().value());
    }

    @Test
    void allowsReadScopeForProjectQueries() {
        var response = http.get().uri("/api/v1/projects")
                .header(HttpHeaders.AUTHORIZATION, "Bearer read-token")
                .retrieve().toEntity(String.class);
        assertEquals(200, response.getStatusCode().value());
    }

    @Test
    void readScopeCannotCreateProjects() {
        RestClientResponseException response = assertThrows(RestClientResponseException.class,
                () -> http.post().uri("/api/v1/projects")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer read-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"name\":\"Read only\",\"description\":\"Must be denied\"}")
                        .retrieve().toBodilessEntity());
        assertEquals(403, response.getStatusCode().value());
    }

    @Test
    void writeScopeCanCreateProjects() {
        var response = http.post().uri("/api/v1/projects")
                .header(HttpHeaders.AUTHORIZATION, "Bearer write-token")
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"name\":\"Authorized\",\"description\":\"Created by test\"}")
                .retrieve().toBodilessEntity();
        assertEquals(201, response.getStatusCode().value());
    }

    private static Jwt jwt(String token, String scope) {
        Instant now = Instant.now();
        return Jwt.withTokenValue(token)
                .header("alg", "RS256")
                .claim("sub", "devforge-security-test")
                .claim("scope", scope)
                .issuedAt(now)
                .expiresAt(now.plusSeconds(300))
                .build();
    }
}

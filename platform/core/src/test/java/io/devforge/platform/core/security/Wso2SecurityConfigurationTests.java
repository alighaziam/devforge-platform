package io.devforge.platform.core.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({"test", "wso2"})
class Wso2SecurityConfigurationTests {

    @Autowired
    private MockMvc mvc;

    @Test
    void rejectsAnonymousApiRequests() throws Exception {
        mvc.perform(get("/api/v1/projects"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsAuthenticatedRequestsWithoutReadScope() throws Exception {
        mvc.perform(get("/api/v1/projects")
                        .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_devforge.write"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void allowsReadScopeForProjectQueries() throws Exception {
        mvc.perform(get("/api/v1/projects")
                        .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_devforge.read"))))
                .andExpect(status().isOk());
    }

    @Test
    void readScopeCannotCreateProjects() throws Exception {
        mvc.perform(post("/api/v1/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Read only\",\"description\":\"Must be denied\"}")
                        .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_devforge.read"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void writeScopeCanCreateProjects() throws Exception {
        mvc.perform(post("/api/v1/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Authorized\",\"description\":\"Created by test\"}")
                        .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_devforge.write"))))
                .andExpect(status().isCreated());
    }
}

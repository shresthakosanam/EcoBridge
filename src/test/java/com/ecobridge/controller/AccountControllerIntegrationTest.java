package com.ecobridge.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:ecobridge-auth-test;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false",
        "FIREBASE_PROJECT_ID="
})
@AutoConfigureMockMvc
class AccountControllerIntegrationTest {
    @Autowired MockMvc mvc;

    @Test
    void emailSignupCreatesSessionAndPasswordLoginWorks() throws Exception {
        mvc.perform(get("/terms.html")).andExpect(status().isOk());
        mvc.perform(get("/privacy.html")).andExpect(status().isOk());

        var signup = mvc.perform(post("/api/auth/signup")
                        .contentType("application/json")
                        .content("{\"name\":\"Test Member\",\"email\":\"member@example.test\",\"password\":\"example-pass-123\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("member@example.test"))
                .andReturn();

        MockHttpSession signupSession = (MockHttpSession) signup.getRequest().getSession(false);
        mvc.perform(get("/api/auth/me").session(signupSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Test Member"));

        mvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content("{\"email\":\"member@example.test\",\"password\":\"wrong-password\"}"))
                .andExpect(status().isUnauthorized());

        var login = mvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content("{\"email\":\"member@example.test\",\"password\":\"example-pass-123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Test Member"))
                .andReturn();

        MockHttpSession loginSession = (MockHttpSession) login.getRequest().getSession(false);
        mvc.perform(get("/api/auth/me").session(loginSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("member@example.test"));
    }
}

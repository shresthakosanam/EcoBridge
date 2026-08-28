package com.ecobridge.config;

import com.ecobridge.repository.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import java.io.IOException;

@Component
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {
    private final UserRepository users;
    public OAuth2LoginSuccessHandler(UserRepository users) { this.users = users; }
    @Override public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                                  Authentication authentication) throws IOException, ServletException {
        OidcUser principal = (OidcUser) authentication.getPrincipal();
        var user = users.findByEmailIgnoreCase(principal.getEmail()).orElseThrow();
        var session = request.getSession(true);
        session.setAttribute("userId", user.getId());
        session.setAttribute("userName", user.getName());
        session.setAttribute("userEmail", user.getEmail());
        response.sendRedirect("/dashboard");
    }
}

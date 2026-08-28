package com.ecobridge.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {
    @Bean
    SecurityFilterChain security(HttpSecurity http, DatabaseOidcUserService oidcUsers,
                                 OAuth2LoginSuccessHandler successHandler) throws Exception {
        return http
                .csrf(csrf -> csrf.ignoringRequestMatchers("/api/**", "/logout", "/h2-console/**"))
                .cors(cors -> {})
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/login", "/login.html", "/signup.html", "/about", "/about.html",
                                "/*.css", "/*.js", "/css/**", "/js/**", "/images/**", "/vendor/**", "/favicon.ico",
                                "/oauth2/**", "/login/oauth2/**", "/actuator/health", "/h2-console/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/events", "/api/posts").permitAll()
                        .requestMatchers("/dashboard", "/dashboard.html", "/eco-pickup/**", "/pickup.html",
                                "/eco-events/**", "/events.html", "/eco-feed/**", "/feed.html", "/profile/**",
                                "/api/**").authenticated()
                        .anyRequest().denyAll())
                .oauth2Login(oauth -> oauth
                        .loginPage("/login")
                        .userInfoEndpoint(info -> info.oidcUserService(oidcUsers))
                        .successHandler(successHandler)
                        .failureUrl("/login?error"))
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/")
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("ECOBRIDGE_SESSION", "JSESSIONID"))
                .exceptionHandling(errors -> errors.authenticationEntryPoint((request, response, exception) -> {
                    if (request.getRequestURI().startsWith("/api/")) response.sendError(401, "Authentication required");
                    else response.sendRedirect("/login");
                }))
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
                .build();
    }
}

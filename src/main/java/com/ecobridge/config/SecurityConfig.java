package com.ecobridge.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {
    @Bean
    SecurityFilterChain security(HttpSecurity http, DatabaseOidcUserService oidcUsers,
                                 OAuth2LoginSuccessHandler successHandler) throws Exception {
        return http
                .csrf(csrf -> csrf.ignoringRequestMatchers(
                        request -> request.getRequestURI().startsWith("/api/")
                                && !request.getRequestURI().startsWith("/api/admin/"),
                        request -> request.getRequestURI().equals("/logout")
                                || request.getRequestURI().startsWith("/h2-console/")))
                .cors(cors -> {})
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/index.html", "/login", "/login.html", "/signup.html", "/about", "/about.html",
                                "/*.css", "/*.js", "/css/**", "/js/**", "/images/**", "/uploads/**", "/media/**", "/vendor/**", "/favicon.ico",
                                "/oauth2/**", "/login/oauth2/**", "/api/auth/**", "/actuator/health", "/h2-console/**", "/error").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/events", "/api/posts").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/admin", "/admin.html").hasRole("ADMIN")
                        .requestMatchers("/api/collector/**").hasRole("COLLECTOR")
                        .requestMatchers("/dashboard", "/dashboard.html", "/eco-pickup/**", "/pickup.html",
                                "/eco-events/**", "/events.html", "/eco-feed/**", "/feed.html", "/profile/**", "/collector", "/collector.html",
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

    @Bean
    PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }
}

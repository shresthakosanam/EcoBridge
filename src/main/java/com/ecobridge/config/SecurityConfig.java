package com.ecobridge.config;
import org.springframework.context.annotation.*; import org.springframework.security.config.annotation.web.builders.HttpSecurity; import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder; import org.springframework.security.crypto.password.PasswordEncoder; import org.springframework.security.web.SecurityFilterChain;
@Configuration public class SecurityConfig {
 @Bean PasswordEncoder passwordEncoder(){return new BCryptPasswordEncoder();}
 @Bean SecurityFilterChain security(HttpSecurity http)throws Exception{return http.csrf(c->c.ignoringRequestMatchers("/api/**","/h2-console/**")).authorizeHttpRequests(a->a.anyRequest().permitAll()).headers(h->h.frameOptions(f->f.sameOrigin())).build();}
}

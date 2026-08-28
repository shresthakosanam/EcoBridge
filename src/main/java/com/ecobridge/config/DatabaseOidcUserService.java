package com.ecobridge.config;

import com.ecobridge.entity.User;
import com.ecobridge.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class DatabaseOidcUserService extends OidcUserService {
    private final UserRepository users;
    public DatabaseOidcUserService(UserRepository users) { this.users = users; }

    @Override @Transactional
    public OidcUser loadUser(OidcUserRequest request) throws OAuth2AuthenticationException {
        OidcUser google = super.loadUser(request);
        String googleId = google.getSubject();
        String email = google.getEmail();
        if (email == null || email.isBlank()) throw new OAuth2AuthenticationException("Google account did not provide an email");
        User user = users.findByGoogleId(googleId)
                .orElseGet(() -> users.findByEmailIgnoreCase(email).orElseGet(User::new));
        user.setGoogleId(googleId);
        user.setEmail(email.toLowerCase());
        user.setName(google.getFullName() == null || google.getFullName().isBlank() ? email : google.getFullName());
        user.setProfileImageUrl(google.getPicture());
        if (user.getRole() == null || user.getRole().isBlank()) user.setRole("ROLE_USER");
        user = users.save(user);
        return new DefaultOidcUser(Set.of(new SimpleGrantedAuthority(user.getRole())),
                google.getIdToken(), google.getUserInfo(), "email");
    }
}

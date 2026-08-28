package com.ecobridge.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

@Service
public class FirebaseService {
    private static final Logger log = LoggerFactory.getLogger(FirebaseService.class);

    private final boolean configured;
    private final String configurationError;

    public FirebaseService(
            @Value("${FIREBASE_PROJECT_ID:}") String projectId,
            @Value("${FIREBASE_SERVICE_ACCOUNT_JSON:}") String serviceJson,
            @Value("${FIREBASE_STORAGE_BUCKET:}") String storageBucket) {
        boolean initialized = false;
        String error = "";

        if (projectId.isBlank()) {
            error = "FIREBASE_PROJECT_ID is missing";
        } else {
            try {
                if (FirebaseApp.getApps().isEmpty()) {
                    GoogleCredentials credentials = serviceJson.isBlank()
                            ? GoogleCredentials.getApplicationDefault()
                            : GoogleCredentials.fromStream(new ByteArrayInputStream(
                                    serviceJson.getBytes(StandardCharsets.UTF_8)));
                    FirebaseOptions options = FirebaseOptions.builder()
                            .setCredentials(credentials)
                            .setProjectId(projectId)
                            .setStorageBucket(storageBucket)
                            .build();
                    FirebaseApp.initializeApp(options);
                }
                initialized = true;
            } catch (Exception exception) {
                error = "Firebase Admin credentials are missing or invalid";
                log.warn("Firebase Admin initialization failed: {}", exception.getMessage());
            }
        }

        configured = initialized;
        configurationError = error;
    }

    public boolean configured() {
        return configured;
    }

    public String configurationError() {
        return configurationError;
    }

}

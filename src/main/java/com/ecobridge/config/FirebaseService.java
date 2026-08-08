package com.ecobridge.config;
import com.google.auth.oauth2.GoogleCredentials; import com.google.firebase.*; import com.google.firebase.auth.*;
import org.springframework.beans.factory.annotation.Value; import org.springframework.stereotype.Service; import java.io.*; import java.nio.charset.StandardCharsets;
@Service public class FirebaseService {
 private final boolean configured;
 public FirebaseService(@Value("${FIREBASE_PROJECT_ID:}") String projectId,@Value("${FIREBASE_SERVICE_ACCOUNT_JSON:}") String serviceJson)throws IOException{configured=!projectId.isBlank();if(configured&&FirebaseApp.getApps().isEmpty()){GoogleCredentials credentials=serviceJson.isBlank()?GoogleCredentials.getApplicationDefault():GoogleCredentials.fromStream(new ByteArrayInputStream(serviceJson.getBytes(StandardCharsets.UTF_8)));var options=FirebaseOptions.builder().setCredentials(credentials).setProjectId(projectId).build();FirebaseApp.initializeApp(options);}}
 public boolean configured(){return configured;} public FirebaseToken verify(String token)throws FirebaseAuthException{if(!configured)throw new IllegalStateException("Firebase Admin is not configured");return FirebaseAuth.getInstance().verifyIdToken(token,true);}
}

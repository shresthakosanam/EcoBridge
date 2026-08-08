package com.ecobridge.controller;
import com.ecobridge.config.FirebaseService; import org.springframework.beans.factory.annotation.Value; import org.springframework.web.bind.annotation.*; import java.util.*;
@RestController @RequestMapping("/api/firebase") public class FirebaseConfigController {
 private final FirebaseService service; private final Map<String,String> config;
 public FirebaseConfigController(FirebaseService s,@Value("${FIREBASE_API_KEY:}")String apiKey,@Value("${FIREBASE_AUTH_DOMAIN:}")String authDomain,@Value("${FIREBASE_PROJECT_ID:}")String projectId,@Value("${FIREBASE_STORAGE_BUCKET:}")String bucket,@Value("${FIREBASE_MESSAGING_SENDER_ID:}")String sender,@Value("${FIREBASE_APP_ID:}")String appId){service=s;config=Map.of("apiKey",apiKey,"authDomain",authDomain,"projectId",projectId,"storageBucket",bucket,"messagingSenderId",sender,"appId",appId);}
 @GetMapping("/config") public Map<String,Object> config(){return Map.of("configured",service.configured()&&!config.get("apiKey").isBlank(),"config",config);}
}

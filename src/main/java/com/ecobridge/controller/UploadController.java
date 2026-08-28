package com.ecobridge.controller;

import com.ecobridge.service.FirebaseStorageService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/uploads")
public class UploadController {
    private final FirebaseStorageService storage;

    public UploadController(FirebaseStorageService storage) {
        this.storage = storage;
    }

    @PostMapping
    public Map<String, Object> upload(@RequestParam MultipartFile file,
                                      @RequestParam(defaultValue = "uploads") String purpose,
                                      HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Please log in first");
        }
        return storage.upload(file, userId, purpose);
    }
}

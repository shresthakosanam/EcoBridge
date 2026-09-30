package com.ecobridge.controller;

import com.ecobridge.repository.StoredImageRepository;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

@RestController
public class MediaController {
    private final StoredImageRepository images;

    public MediaController(StoredImageRepository images) { this.images = images; }

    @GetMapping("/media/{id}")
    public ResponseEntity<byte[]> image(@PathVariable Long id) {
        return images.findById(id)
                .map(image -> ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(image.getContentType()))
                        .cacheControl(CacheControl.maxAge(30, TimeUnit.DAYS).cachePublic())
                        .body(image.getImageData()))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}

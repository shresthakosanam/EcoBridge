package com.ecobridge.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

@Configuration
public class LocalUploadConfig implements WebMvcConfigurer {
    private final String uploadLocation;

    public LocalUploadConfig(@Value("${ecobridge.upload-dir:data/uploads}") String uploadDir) {
        this.uploadLocation = Path.of(uploadDir).toAbsolutePath().normalize().toUri().toString();
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(uploadLocation.endsWith("/") ? uploadLocation : uploadLocation + "/");
    }
}

package com.ecobridge.model;
import jakarta.validation.constraints.NotBlank;
public record Post(Long id, @NotBlank String author, @NotBlank String caption, String activity, String imageUrl, int likes, int comments, boolean liked, String createdAt) {}

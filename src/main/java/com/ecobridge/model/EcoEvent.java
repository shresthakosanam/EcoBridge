package com.ecobridge.model;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
public record EcoEvent(Long id, @NotBlank String name, @NotBlank String description, @NotNull LocalDate date, @NotBlank String time, @NotBlank String location, @Positive int capacity, String imageUrl, String organizer, int registered, boolean joined) {}

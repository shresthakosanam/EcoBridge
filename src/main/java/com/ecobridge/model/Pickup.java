package com.ecobridge.model;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
public record Pickup(Long id, @NotBlank String wasteType, @Positive double quantity, String imageUrl, @NotBlank String address, @NotNull @FutureOrPresent LocalDate preferredDate, @NotBlank String preferredTime, String notes, String status) {}

package com.ecobridge.repository;

import com.ecobridge.entity.StoredImage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StoredImageRepository extends JpaRepository<StoredImage, Long> {}

package com.ecobridge.service;

import com.ecobridge.entity.StoredImage;
import com.ecobridge.repository.StoredImageRepository;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class FirebaseStorageServiceTest {
    @Test
    void storesImageInDatabaseWhenFirebaseBucketIsUnavailable() {
        StoredImageRepository images = mock(StoredImageRepository.class);
        when(images.save(any(StoredImage.class))).thenAnswer(invocation -> {
            StoredImage image = invocation.getArgument(0);
            ReflectionTestUtils.setField(image, "id", 12L);
            return image;
        });
        FirebaseStorageService service = new FirebaseStorageService("", images);
        MockMultipartFile file = new MockMultipartFile("file", "photo.png", "image/png", new byte[]{1, 2, 3});

        Map<String, Object> result = service.upload(file, 7L, "posts");

        assertEquals("/media/12", result.get("url"));
        assertEquals("database", result.get("storage"));
        verify(images).save(argThat(image -> image.getUserId().equals(7L)
                && image.getContentType().equals("image/png")
                && image.getImageData().length == 3));
    }
}

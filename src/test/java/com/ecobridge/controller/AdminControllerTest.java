package com.ecobridge.controller;

import com.ecobridge.entity.Collector;
import com.ecobridge.entity.User;
import com.ecobridge.repository.CollectorRepository;
import com.ecobridge.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminControllerTest {
    @Mock CollectorRepository collectors;
    @Mock UserRepository users;
    @InjectMocks AdminController admin;

    @Test
    void listsApplicationsWithApplicantDetails() {
        Collector collector = pendingCollector();
        User user = member();
        when(collectors.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(collector));
        when(users.findById(7L)).thenReturn(Optional.of(user));

        var applications = admin.applications();

        assertEquals(1, applications.size());
        assertEquals("Member", applications.getFirst().applicantName());
        assertEquals("PENDING", applications.getFirst().status());
    }

    @Test
    void approvingPendingApplicationGrantsCollectorRole() {
        Collector collector = pendingCollector();
        User user = member();
        when(collectors.findById(3L)).thenReturn(Optional.of(collector));
        when(users.findById(7L)).thenReturn(Optional.of(user));

        admin.approve(3L);

        assertEquals("APPROVED", collector.getVerificationStatus());
        assertEquals("ROLE_COLLECTOR", user.getRole());
        verify(collectors).save(collector);
        verify(users).save(user);
    }

    @Test
    void cannotReviewAnApplicationTwice() {
        Collector collector = pendingCollector();
        collector.setVerificationStatus("APPROVED");
        when(collectors.findById(3L)).thenReturn(Optional.of(collector));

        var error = assertThrows(ResponseStatusException.class, () -> admin.reject(3L));

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
    }

    private Collector pendingCollector() {
        Collector collector = new Collector();
        collector.setUserId(7L);
        collector.setPhone("1234567890");
        collector.setServiceArea("Pune");
        return collector;
    }

    private User member() {
        User user = new User();
        user.setName("Member");
        user.setEmail("member@example.com");
        return user;
    }
}

package com.ecommerce.notificationservice.service;

import com.ecommerce.notificationservice.repository.ProcessedEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class IdempotencyServiceTest {

    @Mock
    private ProcessedEventRepository processedEventRepository;

    private IdempotencyService idempotencyService;

    @BeforeEach
    void setUp() {
        idempotencyService =
                new IdempotencyService(processedEventRepository);
    }

    @Test
    void claimEvent_shouldReturnTrueWhenEventIsClaimedSuccessfully() {

        UUID eventId = UUID.randomUUID();

        when(processedEventRepository.claimEvent(
                eq(eventId),
                eq("PAYMENT_SUCCESS"),
                eq("notification-payment-success-group"),
                any(LocalDateTime.class)
        )).thenReturn(1);

        boolean claimed = idempotencyService.claimEvent(
                eventId,
                "PAYMENT_SUCCESS",
                "notification-payment-success-group"
        );

        assertTrue(claimed);

        verify(processedEventRepository).claimEvent(
                eq(eventId),
                eq("PAYMENT_SUCCESS"),
                eq("notification-payment-success-group"),
                any(LocalDateTime.class)
        );
    }

    @Test
    void claimEvent_shouldReturnFalseWhenEventIsDuplicate() {

        UUID eventId = UUID.randomUUID();

        when(processedEventRepository.claimEvent(
                eq(eventId),
                eq("PAYMENT_SUCCESS"),
                eq("notification-payment-success-group"),
                any(LocalDateTime.class)
        )).thenReturn(0);

        boolean claimed = idempotencyService.claimEvent(
                eventId,
                "PAYMENT_SUCCESS",
                "notification-payment-success-group"
        );

        assertFalse(claimed);

        verify(processedEventRepository).claimEvent(
                eq(eventId),
                eq("PAYMENT_SUCCESS"),
                eq("notification-payment-success-group"),
                any(LocalDateTime.class)
        );
    }

    @Test
    void claimEvent_shouldThrowExceptionWhenEventIdIsNull() {

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> idempotencyService.claimEvent(
                                null,
                                "PAYMENT_SUCCESS",
                                "notification-payment-success-group"
                        )
                );

        assertEquals(
                "PAYMENT_SUCCESS eventId cannot be null",
                exception.getMessage()
        );

        verifyNoInteractions(processedEventRepository);
    }
}

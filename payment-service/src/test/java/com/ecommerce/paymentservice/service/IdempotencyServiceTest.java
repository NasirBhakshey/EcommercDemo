package com.ecommerce.paymentservice.service;

import com.ecommerce.paymentservice.repository.ProcessedEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
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
    void claimEvent_shouldReturnTrueWhenEventIsNew() {

        UUID eventId = UUID.randomUUID();

        when(processedEventRepository.claimEvent(
                eq(eventId),
                eq("INVENTORY_RESERVED"),
                eq("payment-inventory-reserved-group"),
                ArgumentMatchers.any(LocalDateTime.class)
        )).thenReturn(1);

        boolean result = idempotencyService.claimEvent(
                eventId,
                "INVENTORY_RESERVED",
                "payment-inventory-reserved-group"
        );

        assertTrue(result);

        verify(processedEventRepository).claimEvent(
                eq(eventId),
                eq("INVENTORY_RESERVED"),
                eq("payment-inventory-reserved-group"),
                ArgumentMatchers.any(LocalDateTime.class)
        );
    }

    @Test
    void claimEvent_shouldReturnFalseWhenEventIsDuplicate() {

        UUID eventId = UUID.randomUUID();

        when(processedEventRepository.claimEvent(
                eq(eventId),
                eq("INVENTORY_RESERVED"),
                eq("payment-inventory-reserved-group"),
                ArgumentMatchers.any(LocalDateTime.class)
        )).thenReturn(0);

        boolean result = idempotencyService.claimEvent(
                eventId,
                "INVENTORY_RESERVED",
                "payment-inventory-reserved-group"
        );

        assertFalse(result);

        verify(processedEventRepository).claimEvent(
                eq(eventId),
                eq("INVENTORY_RESERVED"),
                eq("payment-inventory-reserved-group"),
                ArgumentMatchers.any(LocalDateTime.class)
        );
    }

    @Test
    void claimEvent_shouldThrowExceptionWhenEventIdIsNull() {

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> idempotencyService.claimEvent(
                        null,
                        "INVENTORY_RESERVED",
                        "payment-inventory-reserved-group"
                )
        );

        assertEquals(
                "INVENTORY_RESERVED eventId cannot be null",
                exception.getMessage()
        );

        verifyNoInteractions(processedEventRepository);
    }
}

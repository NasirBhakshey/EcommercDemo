package com.ecommerce.orderservice.service;

import com.ecommerce.orderservice.repository.ProcessedEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class IdempotencyService {

    private final ProcessedEventRepository processedEventRepository;

    public boolean claimEvent(
            UUID eventId,
            String eventType,
            String consumerName) {

        if (eventId == null) {
            throw new IllegalArgumentException(
                    eventType + " eventId cannot be null"
            );
        }

        int inserted = processedEventRepository.claimEvent(
                eventId,
                eventType,
                consumerName,
                LocalDateTime.now()
        );

        return inserted == 1;
    }
}

package com.ecommerce.productservice.service;

import com.ecommerce.productservice.entity.ProcessedEvent;
import com.ecommerce.productservice.repository.ProcessedEventRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class IdempotencyService {

    private final ProcessedEventRepository processedEventRepository;

    public IdempotencyService(
            ProcessedEventRepository processedEventRepository) {
        this.processedEventRepository = processedEventRepository;
    }

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

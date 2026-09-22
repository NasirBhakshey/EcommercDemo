package com.ecommerce.productservice.kafka;

import com.ecommerce.productservice.entity.OutboxEvent;
import com.ecommerce.productservice.event.InventoryFailedEvent;
import com.ecommerce.productservice.event.InventoryReservedEvent;
import com.ecommerce.productservice.repository.OutboxEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class OutboxPublisher {

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public OutboxPublisher(
            OutboxEventRepository outboxEventRepository,
            KafkaTemplate<String, Object> kafkaTemplate,
            ObjectMapper objectMapper
    ) {
        this.outboxEventRepository = outboxEventRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    @Scheduled(fixedDelay = 5000)
    public void publishPendingEvents() {

        List<OutboxEvent> events = outboxEventRepository.findTop100ByPublishedFalseOrderByCreatedAtAsc();

        for (OutboxEvent outboxEvent : events) {

            try {
                Object event;

                if ("INVENTORY_RESERVED".equals(outboxEvent.getEventType())) {

                    event = objectMapper.readValue(
                            outboxEvent.getPayload(),
                            InventoryReservedEvent.class
                    );

                } else if ("INVENTORY_FAILED".equals(outboxEvent.getEventType())) {

                    event = objectMapper.readValue(
                            outboxEvent.getPayload(),
                            InventoryFailedEvent.class
                    );

                } else {

                    System.err.println("Unsupported Product outbox event type: " + outboxEvent.getEventType());
                    continue;
                }

                kafkaTemplate.send(
                        outboxEvent.getTopic(),
                        outboxEvent.getAggregateId(),
                        event
                ).join();

                outboxEvent.setPublished(true);
                outboxEvent.setPublishedAt(LocalDateTime.now());

                outboxEventRepository.save(outboxEvent);

                System.out.println("PRODUCT OUTBOX EVENT PUBLISHED. Type: "
                                + outboxEvent.getEventType()
                                + ", Order ID: "
                                + outboxEvent.getAggregateId());

            } catch (Exception e) {

                System.err.println("PRODUCT OUTBOX EVENT FAILED. ID: " + outboxEvent.getId());

                e.printStackTrace();
            }
        }
    }
}

package com.ecommerce.orderservice.kafka;

import com.ecommerce.orderservice.entity.OutboxEvent;
import com.ecommerce.orderservice.repository.OutboxEventRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class OrderOutboxPublisher {

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, String> outboxKafkaTemplate;

    public OrderOutboxPublisher(
            OutboxEventRepository outboxEventRepository,
            @Qualifier("outboxKafkaTemplate")
            KafkaTemplate<String, String> outboxKafkaTemplate) {

        this.outboxEventRepository = outboxEventRepository;
        this.outboxKafkaTemplate = outboxKafkaTemplate;
    }

    @Scheduled(fixedDelay = 3000)
    @Transactional
    public void publishPendingEvents() {

        List<OutboxEvent> events = outboxEventRepository.findTop50ByPublishedFalseOrderByCreatedAtAsc();

        for (OutboxEvent event : events) {

            try {
                outboxKafkaTemplate.send(
                        event.getTopic(),
                        event.getAggregateId(),
                        event.getPayload()
                ).get();

                event.setPublished(true);
                event.setPublishedAt(LocalDateTime.now());

                outboxEventRepository.save(event);

                System.out.println("ORDER OUTBOX EVENT PUBLISHED. Type: "
                                + event.getEventType()
                                + ", Order ID: "
                                + event.getAggregateId()
                );

            } catch (Exception e) {
                System.err.println("FAILED TO PUBLISH ORDER OUTBOX EVENT. ID: " + event.getId());
            }
        }
    }
}

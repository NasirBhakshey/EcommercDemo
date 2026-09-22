package com.ecommerce.paymentservice.kafka;

import com.ecommerce.paymentservice.entity.OutboxEvent;
import com.ecommerce.paymentservice.event.PaymentFailedEvent;
import com.ecommerce.paymentservice.event.PaymentRefundedEvent;
import com.ecommerce.paymentservice.event.PaymentSuccessEvent;
import com.ecommerce.paymentservice.repository.OutboxEventRepository;
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

    public OutboxPublisher(OutboxEventRepository outboxEventRepository,
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

                if ("PAYMENT_SUCCESS".equals(outboxEvent.getEventType())) {
                    event = objectMapper.readValue(outboxEvent.getPayload(), PaymentSuccessEvent.class);
                } else if ("PAYMENT_FAILED".equals(outboxEvent.getEventType())) {
                    event = objectMapper.readValue(outboxEvent.getPayload(), PaymentFailedEvent.class);
                } else if("PAYMENT_REFUNDED".equals(outboxEvent.getEventType())){
                    event = objectMapper.readValue(outboxEvent.getPayload(), PaymentRefundedEvent.class);
                } else {
                    System.err.println("Unknown outbox event type: " + outboxEvent.getEventType());
                    continue;
                }

                kafkaTemplate.send(
                                outboxEvent.getTopic(),
                                outboxEvent.getAggregateId(),
                                event
                        )
                        .join();

                outboxEvent.setPublished(true);
                outboxEvent.setPublishedAt(
                        LocalDateTime.now()
                );

                outboxEventRepository.save(outboxEvent);

                System.out.println("PAYMENT OUTBOX EVENT PUBLISHED. Type: "
                                + outboxEvent.getEventType()
                                + ", Order ID: "
                                + outboxEvent.getAggregateId());

            } catch (Exception e) {

                System.err.println(
                        "PAYMENT OUTBOX EVENT FAILED. ID: "
                                + outboxEvent.getId()
                );

                e.printStackTrace();
            }
        }
    }
}

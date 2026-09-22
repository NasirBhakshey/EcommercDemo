package com.ecommerce.paymentservice.repository;

import com.ecommerce.paymentservice.entity.ProcessedEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.UUID;

public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent,Long> {

    @Modifying
    @Query(
            value = """
                    INSERT INTO processed_events
                        (event_id, event_type, consumer_name, processed_at)
                    VALUES
                        (:eventId, :eventType, :consumerName, :processedAt)
                    ON CONFLICT (event_id, consumer_name)
                    DO NOTHING
                    """,
            nativeQuery = true
    )
    int claimEvent(
            @Param("eventId") UUID eventId,
            @Param("eventType") String eventType,
            @Param("consumerName") String consumerName,
            @Param("processedAt") LocalDateTime processedAt
    );
}

package com.ecommerce.orderservice.config;

import org.apache.kafka.common.TopicPartition;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DataAccessException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
public class KafkaErrorHandlerConfig {

    @Bean
    public CommonErrorHandler kafkaErrorHandler(
            @Qualifier("outboxKafkaTemplate")
            KafkaTemplate<String, String> kafkaTemplate) {

        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                        kafkaTemplate,
                        (record, exception) ->
                                new TopicPartition(
                                        record.topic() + ".DLT",
                                        record.partition()
                                )
                );

        // 2 seconds between retries, maximum 3 retries
        FixedBackOff backOff = new FixedBackOff(
                        2000L,
                        3L
                );

        DefaultErrorHandler errorHandler = new DefaultErrorHandler(
                        recoverer,
                        backOff
                );

        // Invalid event/data should not repeatedly retry
        errorHandler.addNotRetryableExceptions(IllegalArgumentException.class);

        // Database/infrastructure failures can be retried
        errorHandler.addRetryableExceptions(DataAccessException.class);

        return errorHandler;
    }
}

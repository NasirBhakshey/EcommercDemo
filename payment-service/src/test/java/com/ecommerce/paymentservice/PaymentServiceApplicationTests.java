package com.ecommerce.paymentservice;

import com.ecommerce.paymentservice.repository.OutboxEventRepository;
import com.ecommerce.paymentservice.repository.PaymentRepository;
import com.ecommerce.paymentservice.repository.ProcessedEventRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(
        properties = {
                "spring.autoconfigure.exclude=" +
                        "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration," +
                        "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration," +
                        "org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration," +
                        "org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration"
        }
)
@ActiveProfiles("test")
public class PaymentServiceApplicationTests {

    @MockitoBean
    private PaymentRepository paymentRepository;

    @MockitoBean
    private OutboxEventRepository outboxEventRepository;

    @MockitoBean
    private ProcessedEventRepository processedEventRepository;

    @Test
    void contextLoads() {
    }
}

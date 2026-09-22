package com.ecommerce.orderservice;

import com.ecommerce.orderservice.client.ProductClient;
import com.ecommerce.orderservice.repository.OutboxEventRepository;
import com.ecommerce.orderservice.repository.ProcessedEventRepository;
import com.ecommerce.orderservice.repository.orderRepository;
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
class OrderServiceApplicationTests {

    @MockitoBean
    private orderRepository orderRepository;

    @MockitoBean
    private OutboxEventRepository outboxEventRepository;

    @MockitoBean
    private ProcessedEventRepository processedEventRepository;

    @MockitoBean
    private ProductClient productClient;

    @Test
    void contextLoads() {
    }

}

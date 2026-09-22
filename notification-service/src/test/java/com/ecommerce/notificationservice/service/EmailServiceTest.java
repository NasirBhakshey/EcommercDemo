package com.ecommerce.notificationservice.service;

import com.ecommerce.notificationservice.events.InventoryFailedEvent;
import com.ecommerce.notificationservice.events.PaymentFailedEvent;
import com.ecommerce.notificationservice.events.PaymentSuccessEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    private EmailService emailService;

    @BeforeEach
    void setUp() {

        emailService = new EmailService(mailSender);

        ReflectionTestUtils.setField(
                emailService,
                "fromEmail",
                "noreply@test.com"
        );
    }

    @Test
    void sendPaymentSuccessEmail_shouldSendCorrectEmail() {

        PaymentSuccessEvent event = new PaymentSuccessEvent();

        event.setOrderId(100L);
        event.setPaymentId(200L);
        event.setUserId(10L);
        event.setEmail("customer@test.com");
        event.setAmount(new BigDecimal("1500.00"));
        event.setTransactionId("TXN-123");
        event.setPaidAt(LocalDateTime.of(
                2026, 9, 21, 12, 30
        ));

        emailService.sendPaymentSuccessEmail(event);

        ArgumentCaptor<SimpleMailMessage> captor =
                ArgumentCaptor.forClass(SimpleMailMessage.class);

        verify(mailSender).send(captor.capture());

        SimpleMailMessage message = captor.getValue();

        assertEquals(
                "noreply@test.com",
                message.getFrom()
        );

        assertArrayEquals(
                new String[]{"customer@test.com"},
                message.getTo()
        );

        assertEquals(
                "Payment Successful - Order #100",
                message.getSubject()
        );

        assertNotNull(message.getText());

        assertTrue(message.getText().contains("Order ID: 100"));
        assertTrue(message.getText().contains("Payment ID: 200"));
        assertTrue(message.getText().contains("User ID: 10"));
        assertTrue(message.getText().contains("1500.00"));
        assertTrue(message.getText().contains("TXN-123"));

        verify(mailSender, times(1))
                .send(any(SimpleMailMessage.class));
    }

    @Test
    void sendPaymentFailedEmail_shouldSendCorrectEmail() {

        PaymentFailedEvent event = new PaymentFailedEvent();

        event.setOrderId(100L);
        event.setPaymentId(200L);
        event.setUserId(10L);
        event.setEmail("customer@test.com");
        event.setAmount(new BigDecimal("1500.00"));
        event.setReason("Payment gateway declined");
        event.setFailedAt(LocalDateTime.of(
                2026, 9, 21, 12, 30
        ));

        emailService.sendPaymentFailedEmail(event);

        ArgumentCaptor<SimpleMailMessage> captor =
                ArgumentCaptor.forClass(SimpleMailMessage.class);

        verify(mailSender).send(captor.capture());

        SimpleMailMessage message = captor.getValue();

        assertEquals(
                "noreply@test.com",
                message.getFrom()
        );

        assertArrayEquals(
                new String[]{"customer@test.com"},
                message.getTo()
        );

        assertEquals(
                "Payment Failed - Order #100",
                message.getSubject()
        );

        assertNotNull(message.getText());

        assertTrue(message.getText().contains("Order ID: 100"));
        assertTrue(message.getText().contains("Payment ID: 200"));
        assertTrue(message.getText().contains("User ID: 10"));
        assertTrue(message.getText().contains("1500.00"));
        assertTrue(message.getText().contains(
                "Reason: Payment gateway declined"
        ));

        verify(mailSender, times(1))
                .send(any(SimpleMailMessage.class));
    }

    @Test
    void sendInventoryFailedEmail_shouldSendCorrectEmail() {

        InventoryFailedEvent event = new InventoryFailedEvent();

        event.setOrderId(100L);
        event.setUserId(10L);
        event.setEmail("customer@test.com");
        event.setReason("Insufficient stock");
        event.setFailedAt(LocalDateTime.of(
                2026, 9, 21, 12, 30
        ));

        emailService.sendInventoryFailedEmail(event);

        ArgumentCaptor<SimpleMailMessage> captor =
                ArgumentCaptor.forClass(SimpleMailMessage.class);

        verify(mailSender).send(captor.capture());

        SimpleMailMessage message = captor.getValue();

        assertEquals(
                "noreply@test.com",
                message.getFrom()
        );

        assertArrayEquals(
                new String[]{"customer@test.com"},
                message.getTo()
        );

        assertEquals(
                "Order Could Not Be Processed - Order #100",
                message.getSubject()
        );

        assertNotNull(message.getText());

        assertTrue(
                message.getText().contains("Order ID: 100")
        );

        assertTrue(
                message.getText().contains("User ID: 10")
        );

        assertTrue(
                message.getText().contains(
                        "Reason: Insufficient stock"
                )
        );

        assertTrue(
                message.getText().contains(
                        "Your payment was not processed for this order."
                )
        );

        verify(mailSender, times(1))
                .send(any(SimpleMailMessage.class));
    }

    @Test
    void sendPaymentSuccessEmail_shouldPropagateExceptionWhenMailSendingFails() {

        PaymentSuccessEvent event = new PaymentSuccessEvent();

        event.setOrderId(100L);
        event.setPaymentId(200L);
        event.setUserId(10L);
        event.setEmail("customer@test.com");
        event.setAmount(new BigDecimal("1500.00"));
        event.setTransactionId("TXN-123");
        event.setPaidAt(LocalDateTime.of(
                2026, 9, 21, 12, 30
        ));

        doThrow(new MailSendException("SMTP server unavailable"))
                .when(mailSender)
                .send(any(SimpleMailMessage.class));

        MailSendException exception =
                assertThrows(
                        MailSendException.class,
                        () -> emailService.sendPaymentSuccessEmail(event)
                );

        assertEquals(
                "SMTP server unavailable",
                exception.getMessage()
        );

        verify(mailSender, times(1))
                .send(any(SimpleMailMessage.class));
    }
}

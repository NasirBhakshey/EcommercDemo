package com.ecommerce.notificationservice.service;

import com.ecommerce.notificationservice.events.InventoryFailedEvent;
import com.ecommerce.notificationservice.events.PaymentFailedEvent;
import com.ecommerce.notificationservice.events.PaymentSuccessEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public EmailService(JavaMailSender mailSender){

        this.mailSender = mailSender;
    }

    public void sendPaymentSuccessEmail(PaymentSuccessEvent event) {

        SimpleMailMessage message = new SimpleMailMessage();

        message.setFrom(fromEmail);
        message.setTo(event.getEmail());

        message.setSubject(
                "Payment Successful - Order #" + event.getOrderId()
        );

        message.setText(
                "Your payment was successful.\n\n" +
                        "Order ID: " + event.getOrderId() + "\n" +
                        "Payment ID: " + event.getPaymentId() + "\n" +
                        "User ID: " + event.getUserId() + "\n" +
                        "Amount: ₹" + event.getAmount() + "\n" +
                        "Transaction ID: " + event.getTransactionId() + "\n" +
                        "Paid At: " + event.getPaidAt() + "\n\n" +
                        "Your order payment has been confirmed."
        );

        mailSender.send(message);
    }

    public void sendPaymentFailedEmail(PaymentFailedEvent event) {

        SimpleMailMessage message = new SimpleMailMessage();

        message.setFrom(fromEmail);
        message.setTo(event.getEmail());

        message.setSubject(
                "Payment Failed - Order #" + event.getOrderId()
        );

        message.setText(
                "We could not complete your payment.\n\n" +
                        "Order ID: " + event.getOrderId() + "\n" +
                        "Payment ID: " + event.getPaymentId() + "\n" +
                        "User ID: " + event.getUserId() + "\n" +
                        "Amount: ₹" + event.getAmount() + "\n" +
                        "Reason: " + event.getReason() + "\n" +
                        "Failed At: " + event.getFailedAt() + "\n\n" +
                        "Please try the payment again."
        );

        mailSender.send(message);
    }

    public void sendInventoryFailedEmail(InventoryFailedEvent event) {

        SimpleMailMessage message = new SimpleMailMessage();

        message.setFrom(fromEmail);
        message.setTo(event.getEmail());

        message.setSubject(
                "Order Could Not Be Processed - Order #" + event.getOrderId()
        );

        message.setText(
                "We could not process your order because inventory was unavailable.\n\n" +
                        "Order ID: " + event.getOrderId() + "\n" +
                        "User ID: " + event.getUserId() + "\n" +
                        "Reason: " + event.getReason() + "\n" +
                        "Failed At: " + event.getFailedAt() + "\n\n" +
                        "Your payment was not processed for this order."
        );

        mailSender.send(message);
    }
}

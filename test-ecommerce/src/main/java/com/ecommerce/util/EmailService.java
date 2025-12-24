package com.ecommerce.util;

import com.ecommerce.model.*;
import java.util.*;
import java.io.*;

/**
 * Email servisi - bos catch bloklari ve string concat ornekleri
 */
public class EmailService {

    private List<String> sentEmails = new ArrayList<>();

    /**
     * String concat in loop
     */
    public String composeOrderEmail(Order order) {
        String email = "";
        email += "Sayin " + order.getUser().getFullName() + ",\n\n";
        email += "Siparisininiz alindi.\n\n";
        email += "Siparis Detaylari:\n";
        email += "==================\n";

        for (OrderItem item : order.getItems()) {
            email += "- " + item.getProduct().getName();
            email += " x" + item.getQuantity();
            email += " = " + item.getSubtotal() + " TL\n";
        }

        email += "\nToplam: " + order.getTotalAmount() + " TL\n";
        email += "Odeme Yontemi: " + order.getPaymentMethod() + "\n";
        email += "Teslimat Adresi: " + order.getShippingAddress() + "\n\n";
        email += "Tesekkur ederiz!\n";
        email += "E-Ticaret Ekibi";

        return email;
    }

    /**
     * Empty catch block
     */
    public void sendEmail(String to, String subject, String body) {
        try {
            // Email gonderme simulasyonu
            if (to == null || to.isEmpty()) {
                throw new IllegalArgumentException("Email address required");
            }
            sentEmails.add(to + ": " + subject);
        } catch (Exception e) {
            // Hata yutuldu - kotu pratik
        }
    }

    /**
     * Empty catch block
     */
    public void sendBulkEmails(List<User> users, String subject, String body) {
        for (User user : users) {
            try {
                sendEmail(user.getEmail(), subject, body);
            } catch (Exception e) {
                // Hata yutuldu
            }
        }
    }

    /**
     * Long parameter list
     */
    public void sendPromotionalEmail(String to, String subject, String body,
                                     String templateName, Map<String, String> variables,
                                     boolean trackOpens, boolean trackClicks,
                                     String fromName, String replyTo) {
        try {
            String finalBody = body;
            if (variables != null) {
                for (Map.Entry<String, String> entry : variables.entrySet()) {
                    finalBody = finalBody.replace("{{" + entry.getKey() + "}}", entry.getValue());
                }
            }
            sentEmails.add(to + ": " + subject);
        } catch (Exception e) {
            // Empty catch
        }
    }

    public List<String> getSentEmails() {
        return sentEmails;
    }
}

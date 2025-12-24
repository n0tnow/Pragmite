package com.ecommerce.service;

import com.ecommerce.model.Order;
import com.ecommerce.model.User;
import java.util.*;
import java.io.*;

/**
 * Odeme servisi - kod kokulari iceriyor
 */
public class PaymentService {

    private Map<Long, String> paymentStatuses = new HashMap<>();
    private List<String> transactionLog = new ArrayList<>();

    /**
     * LONG PARAMETER LIST ve HIGH COMPLEXITY
     */
    public boolean processPayment(Order order, String cardNumber, String cardHolder,
                                  String expiryMonth, String expiryYear, String cvv,
                                  String billingAddress, String billingCity, String billingCountry,
                                  String billingPostalCode, boolean saveCard) {

        // Kart numarasi validasyonu - deeply nested
        if (cardNumber != null) {
            if (cardNumber.length() == 16) {
                if (cardNumber.startsWith("4")) {
                    // Visa
                    if (!validateVisaCard(cardNumber)) {
                        return false;
                    }
                } else if (cardNumber.startsWith("5")) {
                    // Mastercard
                    if (!validateMastercard(cardNumber)) {
                        return false;
                    }
                } else if (cardNumber.startsWith("3")) {
                    // Amex
                    if (cardNumber.length() != 15) {
                        return false;
                    }
                } else {
                    return false;
                }
            } else {
                return false;
            }
        } else {
            return false;
        }

        // CVV kontrolu
        if (cvv == null || cvv.length() < 3 || cvv.length() > 4) {
            return false;
        }

        // Tarih kontrolu - magic numbers
        int month = Integer.parseInt(expiryMonth);
        int year = Integer.parseInt(expiryYear);
        if (month < 1 || month > 12) {
            return false;
        }
        if (year < 2024) {
            return false;
        }

        // Odeme islemi simule et
        String transactionId = UUID.randomUUID().toString();
        paymentStatuses.put(order.getId(), "COMPLETED");

        // Log - string concat
        String log = "";
        log += "Transaction: " + transactionId + "\n";
        log += "Order: " + order.getId() + "\n";
        log += "Amount: " + order.getTotalAmount() + "\n";
        log += "Card: ****" + cardNumber.substring(12) + "\n";
        log += "Status: COMPLETED\n";
        transactionLog.add(log);

        return true;
    }

    /**
     * Duplicate code ornegi - benzer validasyon mantigi
     */
    private boolean validateVisaCard(String cardNumber) {
        // Luhn algorithm - simplified
        int sum = 0;
        boolean alternate = false;
        for (int i = cardNumber.length() - 1; i >= 0; i--) {
            int n = Integer.parseInt(cardNumber.substring(i, i + 1));
            if (alternate) {
                n *= 2;
                if (n > 9) {
                    n = (n % 10) + 1;
                }
            }
            sum += n;
            alternate = !alternate;
        }
        return (sum % 10 == 0);
    }

    /**
     * Duplicate code - ayni algorithm tekrar
     */
    private boolean validateMastercard(String cardNumber) {
        // Luhn algorithm - ayni kod tekrar (DRY ihlali)
        int sum = 0;
        boolean alternate = false;
        for (int i = cardNumber.length() - 1; i >= 0; i--) {
            int n = Integer.parseInt(cardNumber.substring(i, i + 1));
            if (alternate) {
                n *= 2;
                if (n > 9) {
                    n = (n % 10) + 1;
                }
            }
            sum += n;
            alternate = !alternate;
        }
        return (sum % 10 == 0);
    }

    /**
     * Empty catch block
     */
    public void refundPayment(Long orderId, double amount) {
        try {
            if (paymentStatuses.containsKey(orderId)) {
                // Refund islemi
                paymentStatuses.put(orderId, "REFUNDED");
                transactionLog.add("Refund: " + orderId + " - " + amount);
            }
        } catch (Exception e) {
            // Hata yutuldu - kotu pratik
        }
    }

    public String getPaymentStatus(Long orderId) {
        return paymentStatuses.getOrDefault(orderId, "UNKNOWN");
    }
}

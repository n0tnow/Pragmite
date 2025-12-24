package com.ecommerce.util;

import com.ecommerce.model.*;
import java.util.*;

/**
 * Fiyat hesaplayici - magic numbers ve complexity ornekleri
 */
public class PriceCalculator {

    // MAGIC NUMBERS
    public double calculateTotal(List<OrderItem> items, String couponCode, String shippingType) {
        double subtotal = 0;

        for (OrderItem item : items) {
            subtotal += item.getPrice() * item.getQuantity();
        }

        // Vergi - magic number
        double tax = subtotal * 0.18;

        // Kargo - magic numbers
        double shipping = 0;
        if (shippingType.equals("standard")) {
            shipping = 9.99;
        } else if (shippingType.equals("express")) {
            shipping = 24.99;
        } else if (shippingType.equals("overnight")) {
            shipping = 49.99;
        }

        // Indirim - magic numbers
        double discount = 0;
        if (couponCode != null) {
            if (couponCode.equals("SAVE10")) {
                discount = subtotal * 0.10;
            } else if (couponCode.equals("SAVE20")) {
                discount = subtotal * 0.20;
            } else if (couponCode.equals("SAVE50")) {
                discount = subtotal * 0.50;
            }
        }

        // Minimum siparis kontrolu - magic number
        if (subtotal < 50) {
            shipping += 5.99; // Ek kargo ucreti
        }

        return subtotal + tax + shipping - discount;
    }

    /**
     * High complexity method
     */
    public double calculateDynamicPrice(Product product, User user, Date purchaseDate,
                                        int quantity, String location, boolean isMember) {
        double basePrice = product.getPrice();
        double finalPrice = basePrice;

        // Indirim hesaplamalari - deeply nested
        if (product.getDiscountPercentage() > 0) {
            finalPrice = basePrice * (1 - product.getDiscountPercentage() / 100);
        }

        // Miktar indirimi
        if (quantity >= 10) {
            finalPrice = finalPrice * 0.90; // %10 indirim
        } else if (quantity >= 5) {
            finalPrice = finalPrice * 0.95; // %5 indirim
        }

        // Uyelik indirimi
        if (isMember) {
            finalPrice = finalPrice * 0.95;
        }

        // Bolge bazli fiyatlandirma - magic numbers
        if (location != null) {
            if (location.equals("istanbul")) {
                // Ek ucret yok
            } else if (location.equals("ankara") || location.equals("izmir")) {
                finalPrice += 5.0;
            } else {
                finalPrice += 10.0;
            }
        }

        // Sezon indirimi
        Calendar cal = Calendar.getInstance();
        cal.setTime(purchaseDate);
        int month = cal.get(Calendar.MONTH);
        if (month == 0 || month == 1) { // Ocak-Subat
            finalPrice = finalPrice * 0.85; // Kis indirimi
        } else if (month == 6 || month == 7) { // Temmuz-Agustos
            finalPrice = finalPrice * 0.90; // Yaz indirimi
        }

        return finalPrice * quantity;
    }
}

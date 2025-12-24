package com.ecommerce.service;

import com.ecommerce.model.*;
import java.util.*;
import java.io.*;

/**
 * GOD CLASS ornegi - cok fazla sorumluluk
 * Siparis yonetimi, envanter, odeme, bildirim, raporlama hepsi burada
 */
public class OrderService {

    private List<Order> orders = new ArrayList<>();
    private List<Product> products = new ArrayList<>();
    private List<User> users = new ArrayList<>();
    private Map<String, Double> taxRates = new HashMap<>();
    private Map<String, Double> shippingRates = new HashMap<>();
    private List<String> notifications = new ArrayList<>();
    private Map<Long, List<Order>> userOrders = new HashMap<>();
    private Map<String, Integer> stockLevels = new HashMap<>();
    private double totalRevenue = 0;
    private int totalOrders = 0;

    // MAGIC NUMBERS ornekleri
    private static final double TAX_RATE = 0.18;

    public OrderService() {
        // Magic numbers
        taxRates.put("electronics", 0.20);
        taxRates.put("food", 0.08);
        taxRates.put("clothing", 0.18);
        shippingRates.put("standard", 9.99);
        shippingRates.put("express", 24.99);
        shippingRates.put("overnight", 49.99);
    }

    /**
     * LONG METHOD ve HIGH COMPLEXITY ornegi
     * Siparis olusturma - cok fazla is yapiyor
     */
    public Order createOrder(User user, List<OrderItem> items, String paymentMethod,
                            String shippingType, String couponCode, boolean giftWrap,
                            String giftMessage, String specialInstructions) {

        // Kullanici kontrolu
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }
        if (user.getEmail() == null || user.getEmail().isEmpty()) {
            throw new IllegalArgumentException("User email is required");
        }
        if (!user.isActive()) {
            throw new IllegalArgumentException("User account is not active");
        }

        // Urun kontrolu
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Order must have at least one item");
        }

        // Stok kontrolu - DEEPLY NESTED CODE
        for (OrderItem item : items) {
            if (item.getProduct() == null) {
                throw new IllegalArgumentException("Product cannot be null");
            }
            if (item.getQuantity() <= 0) {
                throw new IllegalArgumentException("Quantity must be positive");
            }
            if (item.getProduct().getStock() < item.getQuantity()) {
                if (item.getProduct().isActive()) {
                    if (item.getProduct().getCategory().equals("electronics")) {
                        // Elektronik urunler icin ozel kontrol
                        if (item.getQuantity() > 5) {
                            throw new IllegalArgumentException("Cannot order more than 5 electronics");
                        } else {
                            throw new IllegalArgumentException("Insufficient stock for: " + item.getProduct().getName());
                        }
                    } else {
                        throw new IllegalArgumentException("Insufficient stock for: " + item.getProduct().getName());
                    }
                } else {
                    throw new IllegalArgumentException("Product is not available: " + item.getProduct().getName());
                }
            }
        }

        // Odeme yontemi kontrolu
        if (paymentMethod == null || paymentMethod.isEmpty()) {
            throw new IllegalArgumentException("Payment method is required");
        }
        if (!paymentMethod.equals("credit_card") && !paymentMethod.equals("debit_card")
            && !paymentMethod.equals("paypal") && !paymentMethod.equals("bank_transfer")) {
            throw new IllegalArgumentException("Invalid payment method");
        }

        // Siparis olustur
        Order order = new Order();
        order.setId(System.currentTimeMillis());
        order.setUser(user);
        order.setPaymentMethod(paymentMethod);
        order.setShippingAddress(user.getAddress() + ", " + user.getCity() + ", " + user.getCountry());

        // Urunleri ekle
        double subtotal = 0;
        for (OrderItem item : items) {
            order.addItem(item);
            subtotal += item.getPrice() * item.getQuantity();

            // Stok guncelle
            Product p = item.getProduct();
            p.setStock(p.getStock() - item.getQuantity());
        }

        // Vergi hesapla - MAGIC NUMBERS
        double tax = subtotal * 0.18;

        // Kargo ucreti
        double shipping = 0;
        if (shippingType.equals("standard")) {
            shipping = 9.99;
        } else if (shippingType.equals("express")) {
            shipping = 24.99;
        } else if (shippingType.equals("overnight")) {
            shipping = 49.99;
        }

        // Kupon indirimi
        double discount = 0;
        if (couponCode != null && !couponCode.isEmpty()) {
            if (couponCode.equals("SAVE10")) {
                discount = subtotal * 0.10;
            } else if (couponCode.equals("SAVE20")) {
                discount = subtotal * 0.20;
            } else if (couponCode.equals("FREESHIP")) {
                shipping = 0;
            }
        }

        // Hediye paketi
        if (giftWrap) {
            subtotal += 5.99; // Magic number
        }

        // Toplam
        double total = subtotal + tax + shipping - discount;
        order.setTotalAmount(total);

        // Kaydet
        orders.add(order);

        // Kullanici siparislerini guncelle
        if (!userOrders.containsKey(user.getId())) {
            userOrders.put(user.getId(), new ArrayList<>());
        }
        userOrders.get(user.getId()).add(order);

        // Istatistikleri guncelle
        totalRevenue += total;
        totalOrders++;

        // Bildirim gonder
        sendOrderConfirmation(user, order);
        sendInventoryAlert(items);

        return order;
    }

    /**
     * STRING CONCATENATION IN LOOP ornegi
     */
    public String generateOrderReport(List<Order> orderList) {
        String report = "";
        report += "=== SIPARIS RAPORU ===\n";
        report += "Tarih: " + new Date() + "\n";
        report += "========================\n\n";

        for (Order order : orderList) {
            report += "Siparis #" + order.getId() + "\n";
            report += "Musteri: " + order.getUser().getFullName() + "\n";
            report += "Tarih: " + order.getOrderDate() + "\n";
            report += "Durum: " + order.getStatus() + "\n";
            report += "Urunler:\n";

            for (OrderItem item : order.getItems()) {
                report += "  - " + item.getProduct().getName();
                report += " x" + item.getQuantity();
                report += " = " + item.getSubtotal() + " TL\n";
            }

            report += "Toplam: " + order.getTotalAmount() + " TL\n";
            report += "------------------------\n";
        }

        report += "\nToplam Siparis: " + orderList.size() + "\n";

        double totalAmount = 0;
        for (Order o : orderList) {
            totalAmount += o.getTotalAmount();
        }
        report += "Toplam Tutar: " + totalAmount + " TL\n";

        return report;
    }

    /**
     * EMPTY CATCH BLOCK ornegi
     */
    public void exportOrdersToFile(String filename) {
        try {
            FileWriter writer = new FileWriter(filename);
            for (Order order : orders) {
                writer.write(order.getId() + "," + order.getTotalAmount() + "\n");
            }
            writer.close();
        } catch (IOException e) {
            // Empty catch block - BAD PRACTICE
        }
    }

    /**
     * LONG PARAMETER LIST ornegi
     */
    public void updateOrder(Long orderId, String newStatus, Date shippedDate,
                           Date deliveredDate, String trackingNumber, String carrier,
                           String notes, boolean notifyCustomer, String notificationEmail,
                           boolean updateInventory) {
        for (Order order : orders) {
            if (order.getId().equals(orderId)) {
                order.setStatus(newStatus);
                if (shippedDate != null) {
                    order.setShippedDate(shippedDate);
                }
                if (deliveredDate != null) {
                    order.setDeliveredDate(deliveredDate);
                }
                if (notifyCustomer) {
                    notifications.add("Order " + orderId + " status updated to " + newStatus);
                }
                break;
            }
        }
    }

    /**
     * Feature Envy - baska sinifin verilerini cok kullaniyor
     */
    public double calculateUserLoyaltyDiscount(User user) {
        List<Order> userOrderList = userOrders.get(user.getId());
        if (userOrderList == null) return 0;

        double totalSpent = 0;
        for (Order o : userOrderList) {
            totalSpent += o.getTotalAmount();
        }

        // User sinifinda olmasi gereken mantik
        String fullName = user.getFirstName() + " " + user.getLastName();
        String address = user.getAddress() + ", " + user.getCity() + " " + user.getPostalCode();

        if (totalSpent > 10000) {
            return 0.15; // %15 indirim
        } else if (totalSpent > 5000) {
            return 0.10;
        } else if (totalSpent > 1000) {
            return 0.05;
        }
        return 0;
    }

    // Yardimci metodlar
    private void sendOrderConfirmation(User user, Order order) {
        notifications.add("Order confirmation sent to " + user.getEmail());
    }

    private void sendInventoryAlert(List<OrderItem> items) {
        for (OrderItem item : items) {
            if (item.getProduct().getStock() < 10) {
                notifications.add("Low stock alert: " + item.getProduct().getName());
            }
        }
    }

    // Getters
    public List<Order> getOrders() { return orders; }
    public double getTotalRevenue() { return totalRevenue; }
    public int getTotalOrders() { return totalOrders; }
}

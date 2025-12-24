package com.ecommerce.controller;

import com.ecommerce.model.*;
import com.ecommerce.service.*;
import java.util.*;

/**
 * Siparis controller - long parameter list ve complexity ornekleri
 */
public class OrderController {

    private OrderService orderService;
    private ProductService productService;
    private PaymentService paymentService;

    public OrderController() {
        this.orderService = new OrderService();
        this.productService = new ProductService();
        this.paymentService = new PaymentService();
    }

    /**
     * LONG PARAMETER LIST - cok fazla parametre
     */
    public Order createOrder(Long userId, String userEmail, String userName,
                            List<Long> productIds, List<Integer> quantities,
                            String paymentMethod, String cardNumber, String cardExpiry,
                            String cardCvv, String shippingAddress, String shippingCity,
                            String shippingCountry, String shippingPostalCode,
                            String couponCode, boolean giftWrap) {

        // Kullanici olustur
        User user = new User();
        user.setId(userId);
        user.setEmail(userEmail);
        user.setFirstName(userName);
        user.setActive(true);
        user.setAddress(shippingAddress);
        user.setCity(shippingCity);
        user.setCountry(shippingCountry);
        user.setPostalCode(shippingPostalCode);

        // Siparis kalemleri olustur
        List<OrderItem> items = new ArrayList<>();
        for (int i = 0; i < productIds.size(); i++) {
            Product product = productService.findById(productIds.get(i));
            if (product != null) {
                OrderItem item = new OrderItem(product, quantities.get(i));
                items.add(item);
            }
        }

        // Siparis olustur
        return orderService.createOrder(user, items, paymentMethod, "standard",
                                        couponCode, giftWrap, null, null);
    }

    /**
     * High complexity - cok fazla if-else
     */
    public String getOrderStatus(Long orderId, String format, boolean detailed,
                                 boolean includeItems, String language) {
        String result = "";

        // Siparis bul
        Order order = null;
        for (Order o : orderService.getOrders()) {
            if (o.getId().equals(orderId)) {
                order = o;
                break;
            }
        }

        if (order == null) {
            if (language.equals("tr")) {
                return "Siparis bulunamadi";
            } else {
                return "Order not found";
            }
        }

        // Format kontrolu
        if (format.equals("json")) {
            result = "{";
            result += "\"id\": " + order.getId() + ",";
            result += "\"status\": \"" + order.getStatus() + "\",";
            result += "\"total\": " + order.getTotalAmount();
            if (detailed) {
                result += ",\"date\": \"" + order.getOrderDate() + "\"";
                result += ",\"payment\": \"" + order.getPaymentMethod() + "\"";
            }
            if (includeItems) {
                result += ",\"items\": [";
                for (int i = 0; i < order.getItems().size(); i++) {
                    OrderItem item = order.getItems().get(i);
                    result += "{\"name\": \"" + item.getProduct().getName() + "\", \"qty\": " + item.getQuantity() + "}";
                    if (i < order.getItems().size() - 1) {
                        result += ",";
                    }
                }
                result += "]";
            }
            result += "}";
        } else if (format.equals("xml")) {
            result = "<order>";
            result += "<id>" + order.getId() + "</id>";
            result += "<status>" + order.getStatus() + "</status>";
            result += "<total>" + order.getTotalAmount() + "</total>";
            if (detailed) {
                result += "<date>" + order.getOrderDate() + "</date>";
            }
            result += "</order>";
        } else {
            // Plain text
            if (language.equals("tr")) {
                result = "Siparis #" + order.getId() + "\n";
                result += "Durum: " + order.getStatus() + "\n";
                result += "Toplam: " + order.getTotalAmount() + " TL\n";
            } else {
                result = "Order #" + order.getId() + "\n";
                result += "Status: " + order.getStatus() + "\n";
                result += "Total: " + order.getTotalAmount() + " USD\n";
            }
        }

        return result;
    }
}

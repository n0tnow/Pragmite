package com.ecommerce.service;

import com.ecommerce.model.Product;
import java.util.*;

/**
 * Envanter servisi - kod kokulari iceriyor
 */
public class InventoryService {

    private Map<Long, Integer> stockLevels = new HashMap<>();
    private List<String> alerts = new ArrayList<>();

    // MAGIC NUMBERS
    public void checkStockLevels(List<Product> products) {
        for (Product p : products) {
            if (p.getStock() < 10) {
                alerts.add("Low stock: " + p.getName());
            }
            if (p.getStock() == 0) {
                alerts.add("Out of stock: " + p.getName());
            }
            if (p.getStock() > 1000) {
                alerts.add("Overstock: " + p.getName());
            }
        }
    }

    /**
     * O(n^2) karmasiklik - verimsiz stok karsilastirmasi
     */
    public List<Product> findProductsNeedingRestock(List<Product> products, List<Product> allProducts) {
        List<Product> needRestock = new ArrayList<>();

        for (Product p1 : products) {
            for (Product p2 : allProducts) {
                if (p1.getId().equals(p2.getId())) {
                    if (p1.getStock() < 20) { // Magic number
                        if (!needRestock.contains(p1)) {
                            needRestock.add(p1);
                        }
                    }
                }
            }
        }

        return needRestock;
    }

    /**
     * String concat in loop
     */
    public String generateStockReport(List<Product> products) {
        String report = "";
        report += "STOK RAPORU\n";
        report += "===========\n";

        for (Product p : products) {
            report += p.getName() + ": " + p.getStock() + " adet\n";
            if (p.getStock() < 10) {
                report += "  [!] Dusuk stok uyarisi\n";
            }
        }

        return report;
    }

    public void updateStock(Long productId, int quantity) {
        stockLevels.put(productId, quantity);
    }

    public int getStockLevel(Long productId) {
        return stockLevels.getOrDefault(productId, 0);
    }

    public List<String> getAlerts() {
        return alerts;
    }
}

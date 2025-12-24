package com.ecommerce.service;

import com.ecommerce.model.Product;
import java.util.*;

/**
 * Urun servisi - bazi kod kokulari iceriyor
 */
public class ProductService {

    private List<Product> products = new ArrayList<>();
    private Map<String, List<Product>> categoryCache = new HashMap<>();

    // UNUSED IMPORT ve MAGIC NUMBER ornekleri

    public void addProduct(Product product) {
        // Magic number
        if (product.getPrice() < 0.01) {
            throw new IllegalArgumentException("Price must be at least 0.01");
        }
        if (product.getStock() < 0) {
            throw new IllegalArgumentException("Stock cannot be negative");
        }

        products.add(product);
        invalidateCache(product.getCategory());
    }

    /**
     * O(n^2) karmasiklik ornegi - inefficient search
     */
    public List<Product> findSimilarProducts(Product product) {
        List<Product> similar = new ArrayList<>();

        for (Product p1 : products) {
            for (Product p2 : products) {
                if (!p1.getId().equals(p2.getId()) && !p2.getId().equals(product.getId())) {
                    if (p1.getCategory().equals(p2.getCategory())) {
                        if (p1.getCategory().equals(product.getCategory())) {
                            if (Math.abs(p1.getPrice() - product.getPrice()) < 100) {
                                if (!similar.contains(p1)) {
                                    similar.add(p1);
                                }
                            }
                        }
                    }
                }
            }
        }

        return similar;
    }

    /**
     * String concatenation in loop
     */
    public String getProductCatalog() {
        String catalog = "";
        catalog += "URUN KATALOGU\n";
        catalog += "=============\n\n";

        for (Product p : products) {
            catalog += "ID: " + p.getId() + "\n";
            catalog += "Ad: " + p.getName() + "\n";
            catalog += "Fiyat: " + p.getPrice() + " TL\n";
            catalog += "Stok: " + p.getStock() + "\n";
            catalog += "Kategori: " + p.getCategory() + "\n";
            catalog += "---\n";
        }

        return catalog;
    }

    /**
     * Long method - tek metotta cok fazla is
     */
    public Map<String, Object> generateProductStatistics() {
        Map<String, Object> stats = new HashMap<>();

        // Toplam urun sayisi
        stats.put("totalProducts", products.size());

        // Aktif urun sayisi
        int activeCount = 0;
        for (Product p : products) {
            if (p.isActive()) {
                activeCount++;
            }
        }
        stats.put("activeProducts", activeCount);

        // Kategori bazinda sayilar
        Map<String, Integer> categoryCount = new HashMap<>();
        for (Product p : products) {
            String cat = p.getCategory();
            categoryCount.put(cat, categoryCount.getOrDefault(cat, 0) + 1);
        }
        stats.put("byCategory", categoryCount);

        // Fiyat istatistikleri
        double minPrice = Double.MAX_VALUE;
        double maxPrice = 0;
        double totalPrice = 0;
        for (Product p : products) {
            if (p.getPrice() < minPrice) minPrice = p.getPrice();
            if (p.getPrice() > maxPrice) maxPrice = p.getPrice();
            totalPrice += p.getPrice();
        }
        stats.put("minPrice", minPrice);
        stats.put("maxPrice", maxPrice);
        stats.put("avgPrice", products.isEmpty() ? 0 : totalPrice / products.size());

        // Stok istatistikleri
        int totalStock = 0;
        int lowStockCount = 0;
        int outOfStockCount = 0;
        for (Product p : products) {
            totalStock += p.getStock();
            if (p.getStock() == 0) {
                outOfStockCount++;
            } else if (p.getStock() < 10) { // Magic number
                lowStockCount++;
            }
        }
        stats.put("totalStock", totalStock);
        stats.put("lowStockProducts", lowStockCount);
        stats.put("outOfStockProducts", outOfStockCount);

        // Marka istatistikleri
        Map<String, Integer> brandCount = new HashMap<>();
        for (Product p : products) {
            String brand = p.getBrand();
            if (brand != null) {
                brandCount.put(brand, brandCount.getOrDefault(brand, 0) + 1);
            }
        }
        stats.put("byBrand", brandCount);

        // Indirimli urunler
        int discountedCount = 0;
        for (Product p : products) {
            if (p.getDiscountPercentage() > 0) {
                discountedCount++;
            }
        }
        stats.put("discountedProducts", discountedCount);

        return stats;
    }

    /**
     * Empty catch block
     */
    public Product findById(Long id) {
        try {
            for (Product p : products) {
                if (p.getId().equals(id)) {
                    return p;
                }
            }
        } catch (Exception e) {
            // Bos catch blogu - hata yutuldu
        }
        return null;
    }

    public List<Product> getProductsByCategory(String category) {
        if (categoryCache.containsKey(category)) {
            return categoryCache.get(category);
        }

        List<Product> result = new ArrayList<>();
        for (Product p : products) {
            if (p.getCategory().equals(category)) {
                result.add(p);
            }
        }

        categoryCache.put(category, result);
        return result;
    }

    private void invalidateCache(String category) {
        categoryCache.remove(category);
    }

    public List<Product> getAllProducts() {
        return products;
    }
}

package com.ecommerce;

import com.ecommerce.model.*;
import com.ecommerce.service.*;
import com.ecommerce.controller.*;
import java.util.*;

/**
 * Ana giris noktasi - ornek kullanim
 */
public class Main {

    public static void main(String[] args) {
        System.out.println("E-Ticaret Uygulamasi Baslatiliyor...");

        // Servisler
        ProductService productService = new ProductService();
        OrderService orderService = new OrderService();

        // Ornek urunler ekle
        Product laptop = new Product();
        laptop.setId(1L);
        laptop.setName("Gaming Laptop");
        laptop.setPrice(15000.0);
        laptop.setStock(50);
        laptop.setCategory("electronics");
        laptop.setActive(true);

        Product phone = new Product();
        phone.setId(2L);
        phone.setName("Smartphone");
        phone.setPrice(8000.0);
        phone.setStock(100);
        phone.setCategory("electronics");
        phone.setActive(true);

        productService.addProduct(laptop);
        productService.addProduct(phone);

        System.out.println("Urunler eklendi.");
        System.out.println("Katalog:\n" + productService.getProductCatalog());
    }
}

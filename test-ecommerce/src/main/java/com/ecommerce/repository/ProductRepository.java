package com.ecommerce.repository;

import com.ecommerce.model.Product;
import java.util.*;

/**
 * Urun repository - O(n^2) ve O(n^3) karmasiklik ornekleri
 */
public class ProductRepository {

    private List<Product> products = new ArrayList<>();

    public void save(Product product) {
        products.add(product);
    }

    public Product findById(Long id) {
        for (Product p : products) {
            if (p.getId().equals(id)) {
                return p;
            }
        }
        return null;
    }

    /**
     * O(n^2) karmasiklik
     */
    public List<Product> findDuplicates() {
        List<Product> duplicates = new ArrayList<>();

        for (int i = 0; i < products.size(); i++) {
            for (int j = i + 1; j < products.size(); j++) {
                if (products.get(i).getName().equals(products.get(j).getName())) {
                    if (!duplicates.contains(products.get(i))) {
                        duplicates.add(products.get(i));
                    }
                    if (!duplicates.contains(products.get(j))) {
                        duplicates.add(products.get(j));
                    }
                }
            }
        }

        return duplicates;
    }

    /**
     * O(n^3) karmasiklik - cok verimsiz
     */
    public List<List<Product>> findProductTriples(String category) {
        List<List<Product>> triples = new ArrayList<>();

        for (int i = 0; i < products.size(); i++) {
            for (int j = i + 1; j < products.size(); j++) {
                for (int k = j + 1; k < products.size(); k++) {
                    Product p1 = products.get(i);
                    Product p2 = products.get(j);
                    Product p3 = products.get(k);

                    if (p1.getCategory().equals(category) &&
                        p2.getCategory().equals(category) &&
                        p3.getCategory().equals(category)) {

                        List<Product> triple = new ArrayList<>();
                        triple.add(p1);
                        triple.add(p2);
                        triple.add(p3);
                        triples.add(triple);
                    }
                }
            }
        }

        return triples;
    }

    /**
     * String concat in loop
     */
    public String exportToCsv() {
        String csv = "";
        csv += "id,name,price,stock,category\n";

        for (Product p : products) {
            csv += p.getId() + ",";
            csv += p.getName() + ",";
            csv += p.getPrice() + ",";
            csv += p.getStock() + ",";
            csv += p.getCategory() + "\n";
        }

        return csv;
    }

    public List<Product> findAll() {
        return products;
    }

    public List<Product> findByCategory(String category) {
        List<Product> result = new ArrayList<>();
        for (Product p : products) {
            if (p.getCategory().equals(category)) {
                result.add(p);
            }
        }
        return result;
    }
}

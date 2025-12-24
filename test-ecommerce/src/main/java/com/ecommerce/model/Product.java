package com.ecommerce.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Date;

/**
 * Urun modeli - DATA CLASS code smell ornegi
 */
public class Product {
    private Long id;
    private String name;
    private String description;
    private double price;
    private int stock;
    private String category;
    private Date createdAt;
    private Date updatedAt;
    private boolean active;
    private String sku;
    private double weight;
    private String brand;
    private List<String> tags;
    private String imageUrl;
    private double discountPercentage;

    public Product() {
        this.tags = new ArrayList<>();
    }

    // Sadece getter/setter - davranis yok (Data Class smell)
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public int getStock() { return stock; }
    public void setStock(int stock) { this.stock = stock; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }

    public double getWeight() { return weight; }
    public void setWeight(double weight) { this.weight = weight; }

    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }

    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public double getDiscountPercentage() { return discountPercentage; }
    public void setDiscountPercentage(double discountPercentage) { this.discountPercentage = discountPercentage; }
}

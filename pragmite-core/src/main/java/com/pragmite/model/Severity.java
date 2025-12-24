package com.pragmite.model;

/**
 * Kod kokusunun ciddiyet seviyesi.
 */
public enum Severity {
    INFO("Info", 1, "Bilgilendirme"),
    MINOR("Minor", 2, "Düşük öncelik"),
    MAJOR("Major", 3, "Orta öncelik"),
    CRITICAL("Critical", 4, "Yüksek öncelik"),
    BLOCKER("Blocker", 5, "Engel");

    private final String name;
    private final int weight;
    private final String description;

    Severity(String name, int weight, String description) {
        this.name = name;
        this.weight = weight;
        this.description = description;
    }

    public String getName() { return name; }
    public int getWeight() { return weight; }
    public String getDescription() { return description; }

    @Override
    public String toString() {
        return name;
    }
}

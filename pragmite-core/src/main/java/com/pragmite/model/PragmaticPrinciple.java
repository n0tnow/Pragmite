package com.pragmite.model;

/**
 * Pragmatic Programmer prensipleri.
 */
public enum PragmaticPrinciple {
    DRY("Don't Repeat Yourself", "Tekrar eden kod yazma"),
    ORTHOGONALITY("Orthogonality", "Bileşenler birbirinden bağımsız olmalı"),
    CORRECTNESS("Correctness", "Kod doğru ve güvenilir olmalı"),
    PERFORMANCE("Performance", "Kod verimli çalışmalı"),
    READABILITY("Readability", "Kod okunabilir olmalı"),
    MAINTAINABILITY("Maintainability", "Kod bakımı kolay olmalı");

    private final String name;
    private final String description;

    PragmaticPrinciple(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }
}

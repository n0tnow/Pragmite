package com.ecommerce.util;

import java.util.regex.*;

/**
 * Validasyon yardimci sinifi - complexity ve magic number ornekleri
 */
public class ValidationUtils {

    /**
     * High complexity - cok fazla if-else
     */
    public static boolean validateEmail(String email) {
        if (email == null) {
            return false;
        }
        if (email.isEmpty()) {
            return false;
        }
        if (!email.contains("@")) {
            return false;
        }
        if (email.startsWith("@")) {
            return false;
        }
        if (email.endsWith("@")) {
            return false;
        }
        if (email.contains("..")) {
            return false;
        }
        if (email.length() < 5) { // Magic number
            return false;
        }
        if (email.length() > 254) { // Magic number
            return false;
        }

        String[] parts = email.split("@");
        if (parts.length != 2) {
            return false;
        }
        if (parts[0].isEmpty() || parts[1].isEmpty()) {
            return false;
        }
        if (!parts[1].contains(".")) {
            return false;
        }

        return true;
    }

    /**
     * Magic numbers
     */
    public static boolean validatePhoneNumber(String phone) {
        if (phone == null) return false;

        String cleaned = phone.replaceAll("[^0-9]", "");

        // Magic numbers - telefon uzunluklari
        if (cleaned.length() < 10) return false;
        if (cleaned.length() > 15) return false;

        // Turkiye kontrolu
        if (cleaned.startsWith("90")) {
            return cleaned.length() == 12;
        }
        if (cleaned.startsWith("0")) {
            return cleaned.length() == 11;
        }

        return cleaned.length() >= 10;
    }

    /**
     * Magic numbers - kredi karti validasyonu
     */
    public static boolean validateCreditCard(String cardNumber) {
        if (cardNumber == null) return false;

        String cleaned = cardNumber.replaceAll("[^0-9]", "");

        // Magic numbers
        if (cleaned.length() < 13) return false;
        if (cleaned.length() > 19) return false;

        // Luhn algorithm
        int sum = 0;
        boolean alternate = false;
        for (int i = cleaned.length() - 1; i >= 0; i--) {
            int n = Integer.parseInt(cleaned.substring(i, i + 1));
            if (alternate) {
                n *= 2;
                if (n > 9) {
                    n = (n % 10) + 1;
                }
            }
            sum += n;
            alternate = !alternate;
        }

        return (sum % 10 == 0);
    }

    /**
     * Magic numbers - sifre validasyonu
     */
    public static boolean validatePassword(String password) {
        if (password == null) return false;
        if (password.length() < 8) return false; // Magic
        if (password.length() > 128) return false; // Magic

        boolean hasUpper = false;
        boolean hasLower = false;
        boolean hasDigit = false;
        boolean hasSpecial = false;

        for (char c : password.toCharArray()) {
            if (Character.isUpperCase(c)) hasUpper = true;
            if (Character.isLowerCase(c)) hasLower = true;
            if (Character.isDigit(c)) hasDigit = true;
            if (!Character.isLetterOrDigit(c)) hasSpecial = true;
        }

        return hasUpper && hasLower && hasDigit && hasSpecial;
    }
}

package com.example;

import java.util.List;
import java.util.ArrayList;
import java.io.IOException;  // Unused import - should be detected

/**
 * Test class for Pragmite extension demo
 */
public class Calculator {

    // Magic number - should be detected
    private static final int MAX_SIZE = 100;

    // God class candidate - many responsibilities
    private List<Integer> numbers;
    private String lastOperation;

    public Calculator() {
        this.numbers = new ArrayList<>();
    }

    // O(1) complexity - should show green checkmark
    public int add(int a, int b) {
        return a + b;
    }

    // O(n) complexity - should show blue arrow
    public int sum(List<Integer> nums) {
        int total = 0;
        for (int num : nums) {
            total += num;
        }
        return total;
    }

    // O(n²) complexity - should show warning with orange background
    public int[][] multiplyMatrices(int[][] a, int[][] b) {
        int n = a.length;
        int[][] result = new int[n][n];

        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                for (int k = 0; k < n; k++) {
                    result[i][j] += a[i][k] * b[k][j];
                }
            }
        }
        return result;
    }

    // Long method - should be detected
    // Deeply nested code - should be detected
    // High cyclomatic complexity
    public String processData(String input, int mode, boolean flag) {
        if (input == null) {
            return null;
        }

        StringBuilder result = new StringBuilder();

        if (mode == 1) {
            if (flag) {
                for (int i = 0; i < input.length(); i++) {
                    if (i % 2 == 0) {
                        if (Character.isLetter(input.charAt(i))) {
                            result.append(Character.toUpperCase(input.charAt(i)));
                        } else {
                            result.append(input.charAt(i));
                        }
                    } else {
                        if (Character.isDigit(input.charAt(i))) {
                            result.append('X');
                        } else {
                            result.append(input.charAt(i));
                        }
                    }
                }
            } else {
                for (int i = 0; i < input.length(); i++) {
                    result.append(input.charAt(i));
                }
            }
        } else if (mode == 2) {
            for (int i = 0; i < input.length(); i++) {
                result.append(input.charAt(input.length() - 1 - i));
            }
        } else {
            result.append(input);
        }

        return result.toString();
    }

    // Empty catch block - should be detected
    public void riskyOperation() {
        try {
            // Some risky code
            int x = 42 / 0;  // Magic number
            System.out.println("Result of risky operation: " + x);
        } catch (Exception e) {
            // Log the exception to avoid empty catch block
            e.printStackTrace(); // Logging the exception
            // Alternatively, you could rethrow: throw e;
        }
    }

    // String concatenation in loop - should be detected
    public String buildString(List<String> words) {
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            result.append(word).append(" ");
        }
        return result.toString();
    }

    // Duplicated code (similar to buildString)
    public String buildString2(List<String> words) {
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            result.append(word).append(",");
        }
        return result.toString();
    }

    // Magic strings - should be detected
    public String getStatus(int code) {
        if (code == 200) {
            return "OK";  // Magic string
        } else if (code == 404) {
            return "Not Found";  // Magic string
        } else if (code == 500) {
            return "Server Error";  // Magic string
        }
        return "Unknown";
    }
}

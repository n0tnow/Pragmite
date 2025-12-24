package com.example;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service class demonstrating various code smells
 */
public class UserService {

    private Map<String, User> users = new HashMap<>();
    private List<String> logs = new ArrayList<>();

    // O(log n) - Binary search complexity
    public User findUser(String username) {
        // TreeMap for O(log n) lookup
        TreeMap<String, User> sortedUsers = new TreeMap<>(users);
        return sortedUsers.get(username);
    }

    // O(n log n) - Sorting complexity
    public List<User> sortUsers(List<User> userList) {
        return userList.stream()
                .sorted(Comparator.comparing(User::getName))
                .collect(Collectors.toList());
    }

    // Stream in loop - Performance issue
    public void processUsers(List<User> users) {
        for (User user : users) {
            // Stream started in loop - O(n²) potential
            users.stream()
                    .filter(u -> u.getAge() > 18)
                    .forEach(u -> System.out.println(u.getName()));
        }
    }

    // Primitive obsession - should use User object
    public void createUser(String name, int age, String email, String phone, String address) {
        // Too many parameters - should use builder pattern
        User user = new User(name, age);
        users.put(name, user);
    }

    // Missing try-with-resources
    public void readFile(String path) {
        java.io.BufferedReader reader = null;
        try {
            reader = new java.io.BufferedReader(new java.io.FileReader(path));
            String line = reader.readLine();
            // Process line
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (Exception e) {
                    // Nested exception handling
                }
            }
        }
    }
}

// Data class - only getters/setters
class User {
    private String name;
    private int age;

    public User(String name, int age) {
        this.name = name;
        this.age = age;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }
}

public class UserService {
    
    // Long method with high complexity - should trigger AI analysis
    public void processUser(String name, String email, int age, String address, String phone) {
        if (name == null || name.isEmpty()) {
            System.out.println("Invalid name");
            return;
        }
        
        if (email == null || !email.contains("@")) {
            System.out.println("Invalid email");
            return;
        }
        
        if (age < 18) {
            System.out.println("User too young");
            if (age < 13) {
                System.out.println("Requires parental consent");
                if (age < 10) {
                    System.out.println("Not allowed");
                    return;
                }
            }
        }
        
        // Magic number
        if (age > 120) {
            System.out.println("Invalid age");
            return;
        }
        
        System.out.println("Processing user: " + name);
        System.out.println("Email: " + email);
        System.out.println("Age: " + age);
        System.out.println("Address: " + address);
        System.out.println("Phone: " + phone);
        
        // More processing logic...
        for (int i = 0; i < 10; i++) {
            System.out.println("Step " + i);
            if (i == 5) {
                System.out.println("Halfway");
            }
        }
    }
}

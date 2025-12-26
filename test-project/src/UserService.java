public class UserProcessor {

    // Validation message constants
    private static final String ERROR_INVALID_NAME = "Invalid name";

    private static final String ERROR_INVALID_EMAIL = "Invalid email";

    private static final String ERROR_INVALID_AGE = "Invalid age";

    private static final String ERROR_INVALID_ADDRESS = "Invalid address";

    private static final String ERROR_INVALID_PHONE = "Invalid phone";

    // Validation criteria constants
    private static final ınt MIN_AGE = 0;

    private static final ınt MAX_AGE = 150;

    private static final String EMPTY_STRING = "";

    // Email validation constants
    private static final String EMAIL_PATTERN = "@";

    // Phone validation constants (example)
    private static final ınt MIN_PHONE_LENGTH = 10;

    /**
     * Processes user information with validation.
     * Consider breaking this into smaller methods for better maintainability.
     */
    public void processUser(String name, String email, ınt age, String address, String phone) {
        if (name == null || name.isEmpty()) {
            System.out.println(ERROR_INVALID_NAME);
            return;
        }
        if (email == null || email.isEmpty() || !email.contains(EMAIL_PATTERN)) {
            System.out.println(ERROR_INVALID_EMAIL);
            return;
        }
        if (age < MIN_AGE || age > MAX_AGE) {
            System.out.println(ERROR_INVALID_AGE);
            return;
        }
        if (address == null || address.isEmpty()) {
            System.out.println(ERROR_INVALID_ADDRESS);
            return;
        }
        if (phone == null || phone.length() < MIN_PHONE_LENGTH) {
            System.out.println(ERROR_INVALID_PHONE);
            return;
        }
        // Process valid user...
    }
}

package controller;

import model.User;
import model.Student;
import model.Librarian;
import model.Admin;
import model.Teacher;
import model.Staff;
import service.UserService;
import exception.UserNotFoundException;
import exception.InvalidInputException;
import util.PasswordUtil;
import util.ValidationUtil;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * UserController.java
 * Manages user accounts, authentication, and user-related operations.
 * Handles Student, Librarian, and Admin user types.
 */
public class UserController {

    private UserService userService;
    private static final Map<String, ResetToken> passwordResetTokens = new HashMap<>();
    private static final int RESET_CODE_LENGTH = 6;
    private static final int RESET_CODE_EXPIRATION_MINUTES = 10;

    public UserController() {
        this.userService = new UserService();
    }

    private static class ResetToken {
        private final String code;
        private final LocalDateTime expiresAt;

        public ResetToken(String code, LocalDateTime expiresAt) {
            this.code = code;
            this.expiresAt = expiresAt;
        }

        public boolean isExpired() {
            return LocalDateTime.now().isAfter(expiresAt);
        }

        public String getCode() {
            return code;
        }
    }

    // ─── Authentication ───────────────────────────────────────────────────

    /**
     * Authenticates a user by email and password.
     *
     * @param email    The user's email address.
     * @param password The plaintext password.
     * @return The authenticated User object.
     * @throws UserNotFoundException if user not found or credentials invalid.
     */
    public User login(String email, String password) throws UserNotFoundException {
        User user = userService.getUserByEmail(email);
        if (user == null || !user.authenticate(password)) {
            throw new UserNotFoundException(email);
        }
        user.recordLogin();
        userService.updateUser(user);
        return user;
    }

    public User loginById(String libraryId, String password) throws UserNotFoundException {
        User user = userService.getUserByLibraryId(libraryId);
        if (user == null || !user.authenticate(password)) {
            throw new UserNotFoundException(libraryId);
        }
        user.recordLogin();
        userService.updateUser(user);
        return user;
    }

    public User loginByIdOrEmail(String identifier, String password) throws UserNotFoundException, InvalidInputException {
        if (identifier == null || identifier.trim().isEmpty()) {
            throw new InvalidInputException("identifier", "Email or Library ID is required");
        }
        if (password == null || password.trim().isEmpty()) {
            throw new InvalidInputException("password", "Password is required");
        }

        User user = findUserByIdentifier(identifier);

        if (user == null || !user.authenticate(password)) {
            throw new UserNotFoundException(identifier);
        }
        user.recordLogin();
        userService.updateUser(user);
        return user;
    }

    public String sendResetCode(String identifier) throws UserNotFoundException, InvalidInputException {
        User user = findUserByEmail(identifier);

        String code = generateVerificationCode();
        String key = user.getLibraryId().trim().toLowerCase();
        passwordResetTokens.put(key, new ResetToken(code, LocalDateTime.now().plusMinutes(RESET_CODE_EXPIRATION_MINUTES)));

        // In a real system this code would be sent to the user's email.
        System.out.println("Password reset code sent to " + user.getEmail() + ": " + code);
        return code;
    }

    public boolean verifyResetCode(String identifier, String code) throws UserNotFoundException, InvalidInputException {
        if (code == null || code.trim().isEmpty()) {
            throw new InvalidInputException("code", "Verification code is required");
        }
        User user = findUserByEmail(identifier);
        String key = user.getLibraryId().trim().toLowerCase();
        ResetToken token = passwordResetTokens.get(key);
        return token != null && !token.isExpired() && token.getCode().equals(code.trim());
    }

    public boolean resetPasswordWithCode(String identifier, String code, String newPassword)
            throws UserNotFoundException, InvalidInputException {
        if (!ValidationUtil.isValidPassword(newPassword)) {
            throw new InvalidInputException("password", "Password must be at least 4 characters");
        }
        if (!verifyResetCode(identifier, code)) {
            throw new InvalidInputException("code", "Invalid or expired verification code");
        }

        User user = findUserByEmail(identifier);
        user.setPassword(PasswordUtil.hashPassword(newPassword));
        boolean updated = userService.updateUser(user);
        if (updated) {
            passwordResetTokens.remove(user.getLibraryId().trim().toLowerCase());
        }
        return updated;
    }

    private User findUserByEmail(String identifier) throws InvalidInputException, UserNotFoundException {
        if (identifier == null || identifier.trim().isEmpty()) {
            throw new InvalidInputException("identifier", "Email is required for password reset");
        }
        String normalized = identifier.trim();
        if (!normalized.contains("@") || !ValidationUtil.isValidEmail(normalized)) {
            throw new InvalidInputException("identifier", "Password reset is only available using your registered email address");
        }
        User user = userService.getUserByEmail(normalized);
        if (user == null) {
            throw new UserNotFoundException(normalized);
        }
        return user;
    }

    private User findUserByIdentifier(String identifier) throws InvalidInputException {
        if (identifier == null || identifier.trim().isEmpty()) {
            throw new InvalidInputException("identifier", "Email or Library ID is required");
        }
        String normalized = identifier.trim();
        User user = null;
        if (normalized.contains("@")) {
            if (!ValidationUtil.isValidEmail(normalized)) {
                throw new InvalidInputException("identifier", "Invalid email format");
            }
            user = userService.getUserByEmail(normalized);
        } else {
            user = userService.getUserByLibraryId(normalized);
            if (user == null) {
                user = userService.getUserByEmail(normalized);
            }
        }
        return user;
    }

    private String generateVerificationCode() {
        int maxValue = (int) Math.pow(10, RESET_CODE_LENGTH);
        int codeValue = new Random().nextInt(maxValue);
        return String.format("%0" + RESET_CODE_LENGTH + "d", codeValue);
    }

    /**
     * Logs out a user (records logout time).
     *
     * @param userId The user logging out.
     */
    public void logout(String userId) {
        User user = userService.getUser(userId);
        if (user != null) {
            user.logout();
        }
    }

    // ─── User CRUD Operations ────────────────────────────────────────────

    /**
     * Creates a new student account.
     *
     * @param userId  Unique identifier.
     * @param name    Full name.
     * @param email   Email address.
     * @param password Password.
     * @return The created Student object.
     * @throws InvalidInputException if data is invalid.
     */
    public Student createStudent(String userId, String name, String email, String password)
            throws InvalidInputException {
        validateUserInput(email, password);
        Student student = new Student(userId, name, email, password);
        userService.addUser(student);
        return student;
    }

    /**
     * Creates a new teacher account.
     */
    public Teacher createTeacher(String userId, String name, String email, String password)
            throws InvalidInputException {
        validateUserInput(email, password);
        Teacher teacher = new Teacher(userId, name, email, password);
        userService.addUser(teacher);
        return teacher;
    }

    /**
     * Creates a new staff account.
     */
    public Staff createStaff(String userId, String name, String email, String password)
            throws InvalidInputException {
        validateUserInput(email, password);
        Staff staff = new Staff(userId, name, email, password);
        userService.addUser(staff);
        return staff;
    }

    /**
     * Creates a new librarian account.
     *
     * @param userId  Unique identifier.
     * @param name    Full name.
     * @param email   Email address.
     * @param password Password.
     * @return The created Librarian object.
     * @throws InvalidInputException if data is invalid.
     */
    public Librarian createLibrarian(String userId, String name, String email, String password)
            throws InvalidInputException {
        validateUserInput(email, password);
        Librarian librarian = new Librarian(userId, name, email, password);
        userService.addUser(librarian);
        return librarian;
    }

    /**
     * Creates a new admin account.
     *
     * @param userId  Unique identifier.
     * @param name    Full name.
     * @param email   Email address.
     * @param password Password.
     * @return The created Admin object.
     * @throws InvalidInputException if data is invalid.
     */
    public Admin createAdmin(String userId, String name, String email, String password)
            throws InvalidInputException {
        return createAdmin(userId, userId, name, email, password);
    }

    public Admin createAdmin(String userId, String libraryId, String name, String email, String password)
            throws InvalidInputException {
        validateUserInput(email, password);
        Admin admin = new Admin(userId, name, email, password);
        admin.setLibraryId(libraryId);
        userService.addUser(admin);
        return admin;
    }

    /**
     * Retrieves a user by ID.
     *
     * @param userId The user identifier.
     * @return The User object or null if not found.
     */
    public User getUser(String userId) {
        return userService.getUser(userId);
    }

    /**
     * Retrieves a user by email.
     *
     * @param email The user's email address.
     * @return The User object or null if not found.
     */
    public User getUserByEmail(String email) {
        return userService.getUserByEmail(email);
    }

    /**
     * Retrieves a user by library ID.
     *
     * @param libraryId The user's library login identifier.
     * @return The User object or null if not found.
     */
    public User getUserByLibraryId(String libraryId) {
        return userService.getUserByLibraryId(libraryId);
    }

    /**
     * Updates a user's profile.
     *
     * @param user The user with updated information.
     * @return true if successfully updated.
     */
    public boolean updateUser(User user) {
        return userService.updateUser(user);
    }

    /**
     * Deactivates a user account (soft delete).
     *
     * @param userId The user to deactivate.
     * @return true if successfully deactivated.
     */
    public boolean deleteUser(String userId) {
        User user = userService.getUser(userId);
        if (user != null) {
            user.setActive(false);
            return userService.updateUser(user);
        }
        return false;
    }

    /**
     * Resets a user's password (admin only).
     *
     * @param userId       The user whose password to reset.
     * @param newPassword  The new password.
     * @return true if successfully reset.
     */
    public boolean resetPassword(String userId, String newPassword) {
        User user = userService.getUser(userId);
        if (user != null && newPassword != null && !newPassword.isEmpty()) {
            user.setPassword(newPassword);
            return userService.updateUser(user);
        }
        return false;
    }

    // ─── User Listing ────────────────────────────────────────────────────

    /**
     * Retrieves all users of a specific role.
     *
     * @param role The user role (ADMIN, LIBRARIAN, STUDENT).
     * @return List of users with that role.
     */
    public List<User> getUsersByRole(String role) {
        return userService.getUsersByRole(role);
    }

    /**
     * Retrieves all active users.
     *
     * @return List of active users.
     */
    public List<User> getAllUsers() {
        return userService.getAllUsers();
    }

    // ─── Input Validation ────────────────────────────────────────────────

    /**
     * Validates email and password input.
     *
     * @param email The email to validate.
     * @param password The password to validate.
     * @throws InvalidInputException if input is invalid.
     */
    private void validateUserInput(String email, String password) throws InvalidInputException {
        if (email == null || email.isEmpty() || !email.contains("@")) {
            throw new InvalidInputException("email", "Invalid email format");
        }
        if (password == null || password.length() < 4) {
            throw new InvalidInputException("password", "Password must be at least 4 characters");
        }
    }
}

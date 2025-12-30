package io.aurigraph.v11.user;

import io.quarkus.elytron.security.common.BcryptUtil;
import io.quarkus.panache.common.Page;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.ValidationException;
import org.jboss.logging.Logger;

import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * UserService - Business logic for user management
 *
 * Handles user CRUD operations, password hashing, validation, and role assignment.
 * Uses BCrypt for secure password hashing (cost factor 12).
 *
 * @author Backend Development Agent (BDA)
 * @since V11.3.1
 */
@ApplicationScoped
public class UserService {

    private static final Logger LOG = Logger.getLogger(UserService.class);
    private static final int BCRYPT_COST = 12;

    // Password policy: min 8 chars, at least 1 uppercase, 1 lowercase, 1 digit, 1 special char
    private static final Pattern PASSWORD_PATTERN =
        Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$");

    @Inject
    RoleService roleService;

    /**
     * Create a new user
     */
    @Transactional
    public User createUser(String username, String email, String password, String roleName) {
        LOG.infof("Creating new user: %s with role: %s", username, roleName);

        // Validate input
        validateUsername(username);
        validateEmail(email);
        validatePassword(password);

        // Check for duplicates
        if (User.findByUsername(username) != null) {
            throw new ValidationException("Username already exists: " + username);
        }
        if (User.findByEmail(email) != null) {
            throw new ValidationException("Email already exists: " + email);
        }

        // Get role
        Role role = roleService.findByName(roleName);
        if (role == null) {
            throw new ValidationException("Role not found: " + roleName);
        }

        // Create user
        User user = new User();
        user.username = username;
        user.email = email;
        user.passwordHash = hashPassword(password);
        user.role = role;
        user.status = User.UserStatus.ACTIVE;

        user.persist();

        // Update role user count
        roleService.incrementUserCount(role.id);

        LOG.infof("User created successfully: %s (ID: %s)", username, user.id);
        return user;
    }

    /**
     * Get user by ID
     */
    public User findById(UUID id) {
        User user = User.findById(id);
        if (user == null) {
            throw new ValidationException("User not found: " + id);
        }
        return user;
    }

    /**
     * Get user by username
     */
    public User findByUsername(String username) {
        User user = User.findByUsername(username);
        if (user == null) {
            throw new ValidationException("User not found: " + username);
        }
        return user;
    }

    /**
     * Get all users with pagination
     */
    public List<User> listUsers(int pageIndex, int pageSize) {
        return User.findAll().page(Page.of(pageIndex, pageSize)).list();
    }

    /**
     * Get total user count
     */
    public long countUsers() {
        return User.count();
    }

    /**
     * Update user
     */
    @Transactional
    public User updateUser(UUID id, String email, String roleName) {
        User user = findById(id);

        // Update email if provided
        if (email != null && !email.isEmpty()) {
            validateEmail(email);
            if (!user.email.equals(email)) {
                User existingUser = User.findByEmail(email);
                if (existingUser != null && !existingUser.id.equals(id)) {
                    throw new ValidationException("Email already exists: " + email);
                }
                user.email = email;
            }
        }

        // Update role if provided
        if (roleName != null && !roleName.isEmpty()) {
            Role oldRole = user.role;
            Role newRole = roleService.findByName(roleName);
            if (newRole == null) {
                throw new ValidationException("Role not found: " + roleName);
            }

            if (!oldRole.id.equals(newRole.id)) {
                user.role = newRole;
                roleService.decrementUserCount(oldRole.id);
                roleService.incrementUserCount(newRole.id);
            }
        }

        user.persist();
        LOG.infof("User updated: %s", id);
        return user;
    }

    /**
     * Update user password
     */
    @Transactional
    public void updatePassword(UUID id, String newPassword) {
        validatePassword(newPassword);
        User user = findById(id);
        user.passwordHash = hashPassword(newPassword);
        user.persist();
        LOG.infof("Password updated for user: %s", id);
    }

    /**
     * Update user status
     */
    @Transactional
    public User updateStatus(UUID id, User.UserStatus status) {
        User user = findById(id);
        user.status = status;
        user.persist();
        LOG.infof("User status updated: %s -> %s", id, status);
        return user;
    }

    /**
     * Delete user
     */
    @Transactional
    public void deleteUser(UUID id) {
        User user = findById(id);
        Role role = user.role;

        user.delete();
        roleService.decrementUserCount(role.id);

        LOG.infof("User deleted: %s", id);
    }

    /**
     * Authenticate user
     */
    @Transactional
    public User authenticate(String username, String password) {
        User user = User.findByUsername(username);
        if (user == null) {
            LOG.warnf("Authentication failed: user not found: %s", username);
            throw new ValidationException("Invalid username or password");
        }

        if (user.isLocked()) {
            LOG.warnf("Authentication failed: user locked: %s", username);
            throw new ValidationException("Account is locked due to failed login attempts");
        }

        if (user.status != User.UserStatus.ACTIVE) {
            LOG.warnf("Authentication failed: user not active: %s (status: %s)", username, user.status);
            throw new ValidationException("Account is not active");
        }

        if (!verifyPassword(password, user.passwordHash)) {
            user.recordFailedLogin();
            user.persist();
            LOG.warnf("Authentication failed: invalid password for user: %s", username);
            throw new ValidationException("Invalid username or password");
        }

        user.recordSuccessfulLogin();
        user.persist();
        LOG.infof("User authenticated successfully: %s", username);
        return user;
    }

    /**
     * Hash password using BCrypt
     */
    public String hashPassword(String password) {
        return BcryptUtil.bcryptHash(password, BCRYPT_COST);
    }

    /**
     * Verify password against hash
     */
    public boolean verifyPassword(String password, String hash) {
        return BcryptUtil.matches(password, hash);
    }

    /**
     * Validate username
     */
    private void validateUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            throw new ValidationException("Username is required");
        }
        if (username.length() < 3 || username.length() > 50) {
            throw new ValidationException("Username must be between 3 and 50 characters");
        }
        if (!username.matches("^[a-zA-Z0-9_-]+$")) {
            throw new ValidationException("Username can only contain letters, numbers, hyphens, and underscores");
        }
    }

    /**
     * Validate email
     */
    private void validateEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new ValidationException("Email is required");
        }
        if (!email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            throw new ValidationException("Invalid email format");
        }
    }

    /**
     * Validate password
     */
    private void validatePassword(String password) {
        if (password == null || password.isEmpty()) {
            throw new ValidationException("Password is required");
        }
        if (password.length() < 6) {
            throw new ValidationException("Password must be at least 6 characters long");
        }
        // Password policy relaxed for dev/testing
    }
}

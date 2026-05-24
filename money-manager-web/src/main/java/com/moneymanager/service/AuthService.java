package com.moneymanager.service;

import at.favre.lib.crypto.bcrypt.BCrypt;
import com.moneymanager.model.User;
import com.moneymanager.repository.IUserRepo;

import java.util.Optional;

public class AuthService {

    private final IUserRepo userRepo;

    public AuthService(IUserRepo userRepo) {
        this.userRepo = userRepo;
    }

    /**
     * Register a new user. Returns the saved user on success.
     *
     * @throws IllegalArgumentException if registration details are invalid or username is taken
     */
    public User register(String username, String password, String confirmPassword, String recoveryCode) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username must not be blank");
        }
        String normalizedUsername = username.trim();
        if (normalizedUsername.length() > 50) {
            throw new IllegalArgumentException("Username must be 50 characters or fewer");
        }
        validateNewPassword(password);
        if (!password.equals(confirmPassword)) {
            throw new IllegalArgumentException("Passwords do not match");
        }
        validateRecoveryCode(recoveryCode);
        if (userRepo.findByUsername(normalizedUsername).isPresent()) {
            throw new IllegalArgumentException("Username already taken");
        }

        var user = new User();
        user.setUsername(normalizedUsername);
        user.setPasswordHash(hash(password));
        user.setRecoveryCodeHash(hash(recoveryCode));
        return userRepo.save(user);
    }

    /**
     * Authenticate a user by username and password.
     *
     * @return the User if credentials are valid, or empty if not
     */
    public Optional<User> login(String username, String password) {
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            return Optional.empty();
        }
        return userRepo.findByUsername(username.trim())
                .filter(user -> {
                    var result = BCrypt.verifyer().verify(password.toCharArray(), user.getPasswordHash());
                    return result.verified;
                });
    }

    /**
     * Reset a password only when the private recovery code set during registration is supplied.
     */
    public void resetPassword(String username, String recoveryCode, String newPassword, String confirmPassword) {
        validateNewPassword(newPassword);
        if (!newPassword.equals(confirmPassword)) {
            throw new IllegalArgumentException("New passwords do not match");
        }
        if (username == null || username.isBlank() || recoveryCode == null || recoveryCode.isBlank()) {
            throw invalidRecoveryDetails();
        }

        User user = userRepo.findByUsername(username.trim()).orElseThrow(this::invalidRecoveryDetails);
        if (user.getRecoveryCodeHash() == null
                || !BCrypt.verifyer().verify(recoveryCode.toCharArray(), user.getRecoveryCodeHash()).verified) {
            throw invalidRecoveryDetails();
        }

        userRepo.updatePassword(user.getUserId(), hash(newPassword));
    }

    private String hash(String value) {
        return BCrypt.withDefaults().hashToString(12, value.toCharArray());
    }

    private void validateNewPassword(String password) {
        if (password == null || password.length() < 4) {
            throw new IllegalArgumentException("Password must be at least 4 characters");
        }
    }

    private void validateRecoveryCode(String recoveryCode) {
        if (recoveryCode == null || recoveryCode.length() < 6) {
            throw new IllegalArgumentException("Recovery code must be at least 6 characters");
        }
    }

    private IllegalArgumentException invalidRecoveryDetails() {
        return new IllegalArgumentException("Unable to reset password. Check your username and recovery code.");
    }
}

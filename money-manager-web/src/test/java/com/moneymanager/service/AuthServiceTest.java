package com.moneymanager.service;

import com.moneymanager.model.User;
import com.moneymanager.repository.IUserRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthServiceTest {

    private FakeUserRepo repo;
    private AuthService service;

    @BeforeEach
    void setUp() {
        repo = new FakeUserRepo();
        service = new AuthService(repo);
    }

    @Test
    void registrationHashesSecretsAndNormalizesUsernameForLogin() {
        User user = service.register("  maria  ", "pass123", "pass123", "rescue-key");

        assertNotEquals("pass123", user.getPasswordHash());
        assertNotEquals("rescue-key", user.getRecoveryCodeHash());
        assertTrue(service.login(" maria ", "pass123").isPresent());
    }

    @Test
    void resetPasswordRequiresRecoveryCodeAndReplacesPassword() {
        service.register("maria", "pass123", "pass123", "rescue-key");

        assertThrows(IllegalArgumentException.class,
                () -> service.resetPassword("maria", "wrong-key", "newpass", "newpass"));

        service.resetPassword("maria", "rescue-key", "newpass", "newpass");

        assertFalse(service.login("maria", "pass123").isPresent());
        assertTrue(service.login("maria", "newpass").isPresent());
    }

    @Test
    void registrationRejectsPasswordMismatchAndShortRecoveryCode() {
        assertThrows(IllegalArgumentException.class,
                () -> service.register("maria", "pass123", "different", "rescue-key"));
        assertThrows(IllegalArgumentException.class,
                () -> service.register("maria", "pass123", "pass123", "tiny"));
    }

    private static final class FakeUserRepo implements IUserRepo {
        private final Map<String, User> users = new HashMap<>();
        private long nextUserId = 1L;

        @Override
        public User save(User user) {
            user.setUserId(nextUserId++);
            users.put(user.getUsername(), user);
            return user;
        }

        @Override
        public Optional<User> findByUsername(String username) {
            return Optional.ofNullable(users.get(username));
        }

        @Override
        public void updatePassword(long userId, String passwordHash) {
            users.values().stream()
                    .filter(user -> user.getUserId() == userId)
                    .findFirst()
                    .orElseThrow()
                    .setPasswordHash(passwordHash);
        }
    }
}

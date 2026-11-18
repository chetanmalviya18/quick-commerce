package com.quickcommerce.user.service;

import com.quickcommerce.user.dto.requestDto.LoginRequest;
import com.quickcommerce.user.dto.requestDto.RefreshRequest;
import com.quickcommerce.user.dto.requestDto.RegisterRequest;
import com.quickcommerce.user.dto.responseDto.TokenResponse;
import com.quickcommerce.user.exception.EmailAlreadyUsedException;
import com.quickcommerce.user.exception.InvalidCredentialsException;
import com.quickcommerce.user.exception.InvalidRoleException;
import com.quickcommerce.user.model.Role;
import com.quickcommerce.user.model.User;
import com.quickcommerce.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository users;

    @Mock
    private TokenService tokens;

    @Mock
    private RefreshTokenService refresh;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @Captor
    private ArgumentCaptor<User> userCaptor;

    private AuthService service;

    @BeforeEach
    void setUp() {
        service = new AuthService(users, encoder, tokens, refresh);
    }

    private User userWithId(String email, Role role, long id) {
        User user = new User(email, encoder.encode("password123"), role);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    @Test
    @DisplayName("register() stores hashed password and returns access & refresh tokens")
    void registerStoresHashedPasswordAndReturnsTokens() {
        when(users.existsByEmail("a@b.com")).thenReturn(false);
        when(users.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            ReflectionTestUtils.setField(u, "id", 7L);
            return u;
        });
        when(tokens.issueAccessToken(7L, Role.CUSTOMER)).thenReturn("access-1");
        when(tokens.expiresInSeconds()).thenReturn(900L);
        when(refresh.issue(7L)).thenReturn("refresh-1");

        TokenResponse res = service.register(new RegisterRequest("a@b.com", "password123", null));

        verify(users).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();

        assertNotEquals("password123", savedUser.getPasswordHash());
        assertEquals(Role.CUSTOMER, savedUser.getRole());
        assertEquals("access-1", res.accessToken());
        assertEquals("refresh-1", res.refreshToken());
        assertEquals(900L, res.expiresIn());
    }

    @Test
    @DisplayName("register() throws EmailAlreadyUsedException if email already exists")
    void registerRejectsDuplicateEmail() {
        when(users.existsByEmail("a@b.com")).thenReturn(true);
        assertThrows(EmailAlreadyUsedException.class,
                () -> service.register(new RegisterRequest("a@b.com", "password123", null)));
    }

    @Test
    @DisplayName("register() throws InvalidRoleException if attempting to register as ADMIN")
    void registerRejectsAdminRole() {
        assertThrows(InvalidRoleException.class,
                () -> service.register(new RegisterRequest("a@b.com", "password123", Role.ADMIN)));
    }

    @Test
    @DisplayName("login() with incorrect password throws InvalidCredentialsException")
    void loginWithWrongPasswordFails() {
        when(users.findByEmail("a@b.com")).thenReturn(Optional.of(userWithId("a@b.com", Role.CUSTOMER, 1L)));
        assertThrows(InvalidCredentialsException.class,
                () -> service.login(new LoginRequest("a@b.com", "wrong-password")));
    }

    @Test
    @DisplayName("login() with valid credentials returns access & refresh tokens")
    void loginWithCorrectPasswordReturnsTokens() {
        when(users.findByEmail("r@b.com")).thenReturn(Optional.of(userWithId("r@b.com", Role.RIDER, 9L)));
        when(tokens.issueAccessToken(9L, Role.RIDER)).thenReturn("rider-access");
        when(refresh.issue(9L)).thenReturn("rider-refresh");

        TokenResponse res = service.login(new LoginRequest("r@b.com", "password123"));

        assertEquals("rider-access", res.accessToken());
        assertEquals("rider-refresh", res.refreshToken());
        assertEquals("Bearer", res.tokenType());
    }

    @Test
    @DisplayName("refresh() rotates refresh token and returns new token pair for user")
    void refreshReturnsNewTokensForTheSameUser() {
        when(refresh.rotate("old")).thenReturn(new RefreshTokenService.Rotated(9L, "refresh-2"));
        when(users.findById(9L)).thenReturn(Optional.of(userWithId("r@b.com", Role.RIDER, 9L)));
        when(tokens.issueAccessToken(9L, Role.RIDER)).thenReturn("access-2");

        TokenResponse res = service.refreshToken(new RefreshRequest("old"));

        assertEquals("access-2", res.accessToken());
        assertEquals("refresh-2", res.refreshToken());
    }

    @Test
    @DisplayName("logout() revokes the refresh token")
    void logoutRevokesTheRefreshToken() {
        service.logout(new RefreshRequest("some-token"));
        verify(refresh).revoke("some-token");
    }
}

package com.quickcommerce.user.service;

import com.quickcommerce.user.dto.responseDto.UserResponse;
import com.quickcommerce.user.exception.UserNotFoundException;
import com.quickcommerce.user.model.Role;
import com.quickcommerce.user.model.User;
import com.quickcommerce.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository users;

    @InjectMocks
    private UserService userService;

    @Test
    @DisplayName("getUserProfile returns user profile when user exists")
    void getUserProfileSuccess() {
        User user = new User("user@example.com", "hashed", Role.CUSTOMER);
        ReflectionTestUtils.setField(user, "id", 10L);

        when(users.findById(10L)).thenReturn(Optional.of(user));

        UserResponse res = userService.getUserProfile(10L);

        assertNotNull(res);
        assertEquals(10L, res.id());
        assertEquals("user@example.com", res.email());
        assertEquals("CUSTOMER", res.role());
    }

    @Test
    @DisplayName("getUserProfile throws UserNotFoundException when user does not exist")
    void getUserProfileNotFound() {
        when(users.findById(99L)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> userService.getUserProfile(99L));
    }
}

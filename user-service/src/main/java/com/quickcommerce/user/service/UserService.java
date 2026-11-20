package com.quickcommerce.user.service;

import com.quickcommerce.user.dto.responseDto.UserResponse;
import com.quickcommerce.user.exception.UserNotFoundException;
import com.quickcommerce.user.model.User;
import com.quickcommerce.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository users;

    @Transactional(readOnly = true)
    public UserResponse getUserProfile(long userId) {
        User user = users.findById(userId).orElseThrow(UserNotFoundException::new);
        return new UserResponse(user.getId(), user.getEmail(), user.getRole().name());
    }
}

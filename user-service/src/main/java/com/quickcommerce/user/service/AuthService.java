package com.quickcommerce.user.service;

import com.quickcommerce.user.dto.requestDto.LoginRequest;
import com.quickcommerce.user.dto.requestDto.RefreshRequest;
import com.quickcommerce.user.dto.requestDto.RegisterRequest;
import com.quickcommerce.user.dto.responseDto.TokenResponse;
import com.quickcommerce.user.exception.EmailAlreadyUsedException;
import com.quickcommerce.user.exception.InvalidCredentialsException;
import com.quickcommerce.user.exception.InvalidRoleException;
import com.quickcommerce.user.exception.InvalidTokenException;
import com.quickcommerce.user.model.Role;
import com.quickcommerce.user.model.User;
import com.quickcommerce.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final TokenService tokens;
    private final RefreshTokenService refresh;

    @Transactional
    public TokenResponse register(RegisterRequest req) {
        Role role = req.role() == null ? Role.CUSTOMER : req.role();

        if (role == Role.ADMIN || role == Role.SERVICE) {
            log.warn("Registration refused: privileged role requested");
            throw new InvalidRoleException();
        }
        if(users.existsByEmail(req.email())){
            log.warn("Registration refused: email already exists");
            throw new EmailAlreadyUsedException();
        }
        User savedUser = users.save(new User(req.email(), encoder.encode(req.password()), role));
        log.info("User registered: id={}, role={}", savedUser.getId(), savedUser.getRole());
        return respond(savedUser, refresh.issue(savedUser.getId()));
    }

    @Transactional
    public TokenResponse login(LoginRequest req) {
        User user = users.findByEmail(req.email()).orElse(null);
        if (user == null || !encoder.matches(req.password(), user.getPasswordHash())) {
            log.warn("Login failed: unknown email or wrong password");
            throw new InvalidCredentialsException();
        }
        log.info("User logged in: id={}", user.getId());
        return respond(user, refresh.issue(user.getId()));
    }

    @Transactional(noRollbackFor = InvalidTokenException.class)
    public TokenResponse refreshToken(RefreshRequest req) {
        RefreshTokenService.Rotated rotated = refresh.rotate(req.refreshToken());
        User user = users.findById(rotated.userId()).orElseThrow(InvalidTokenException::new);
        log.info("Tokens refreshed: user id={}", user.getId());
        return respond(user, rotated.refreshToken());
    }

    @Transactional
    public void logout(RefreshRequest req) {
        refresh.revoke(req.refreshToken());
        log.info("Logout processed");
    }

    private TokenResponse respond(User user, String refreshToken) {
        return new TokenResponse(tokens.issueAccessToken(user.getId(), user.getRole()), refreshToken,
                "Bearer", tokens.expiresInSeconds());
    }
}

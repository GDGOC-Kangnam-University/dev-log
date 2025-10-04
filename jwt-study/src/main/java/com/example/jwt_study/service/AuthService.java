package com.example.jwt_study.service;

import com.example.jwt_study.domain.User;
import com.example.jwt_study.exception.DuplicateUsernameException;
import com.example.jwt_study.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 인증 서비스 (회원가입, 로그인)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * 회원가입 (BCrypt 암호화)
     */
    @Transactional
    public User register(String username, String password) {
        if (userRepository.existsByUsername(username)) {
            throw new DuplicateUsernameException("이미 사용 중인 사용자명입니다");
        }

        String hashedPassword = passwordEncoder.encode(password);
        User user = User.builder()
                .username(username)
                .password(hashedPassword)
                .build();

        User savedUser = userRepository.save(user);
        log.info("회원가입 완료: userId={}, username={}", savedUser.getId(), savedUser.getUsername());

        return savedUser;
    }

    /**
     * 로그인 (비밀번호 검증)
     */
    public User authenticate(String username, String password) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BadCredentialsException("사용자명 또는 비밀번호가 올바르지 않습니다"));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new BadCredentialsException("사용자명 또는 비밀번호가 올바르지 않습니다");
        }

        log.info("로그인 성공: userId={}, username={}", user.getId(), user.getUsername());
        return user;
    }
}
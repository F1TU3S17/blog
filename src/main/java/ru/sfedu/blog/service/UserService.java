package ru.sfedu.blog.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.sfedu.blog.dto.LoginRequest;
import ru.sfedu.blog.dto.RegisterRequest;
import ru.sfedu.blog.entity.User;
import ru.sfedu.blog.exception.AppErrorEnum;
import ru.sfedu.blog.exception.AppException;
import ru.sfedu.blog.repository.UserRepositoryImplt;
import ru.sfedu.blog.security.CustomUserDetails;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {
    private final UserRepositoryImplt userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UserDetails registerUser(RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new AppException(AppErrorEnum.USERNAME_ALREADY_EXISTS);
        }

        if (userRepository.existsByEmail(request.email())) {
            throw new AppException(AppErrorEnum.EMAIL_ALREADY_EXISTS);
        }

        String passwordHash = passwordEncoder.encode(request.password());

        User user = new User(request.username(), request.email(), passwordHash);
        user = userRepository.save(user);

        return new CustomUserDetails(user);

    }

    public UserDetails loginUser(LoginRequest loginRequest) {
        User user = userRepository.getByLogin(loginRequest.loginInput());
        if (!passwordEncoder.matches(loginRequest.password(), user.getPasswordHash())) {
            throw new AppException(AppErrorEnum.USER_PASSWORD_INCORRECT);
        }

        return new CustomUserDetails(user);
    }


}

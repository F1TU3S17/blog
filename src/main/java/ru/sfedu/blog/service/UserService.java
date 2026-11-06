package ru.sfedu.blog.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
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

    public UserDetails registerUser(RegisterRequest request) {
        String passwordHash = passwordEncoder.encode(request.password());

        User user = new User(request.username(), request.email(), passwordHash);
        user = userRepository.save(user);

        return new CustomUserDetails(user);

    }

    public UserDetails loginUser(LoginRequest loginRequest) {
        User user = userRepository.getByLoginInput(loginRequest.loginInput());
        if (!passwordEncoder.matches(loginRequest.password(), user.getPasswordHash())) {
            throw new AppException(AppErrorEnum.USER_PASSWORD_INCORRECT);
        }

        return new CustomUserDetails(user);
    }


}

package ru.sfedu.blog.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import ru.sfedu.blog.entity.User;
import ru.sfedu.blog.exception.AppErrorEnum;
import ru.sfedu.blog.exception.AppException;

@Repository
@RequiredArgsConstructor
@Slf4j
public class UserRepositoryImplt {
    private final UserRepository userRepository;

    @Transactional
    public User save(User user) {
        user = userRepository.save(user);
        log.info("User {} saved with id: {}", user.getUsername(), user.getId());
        return user;
    }

    public User getByLogin(String login) {
        log.info("Find user by login: {}", login);
        return userRepository.findByLoginInput(login).orElseThrow(() -> {
            log.warn("User with login: {} is not found", login);
            return new AppException(AppErrorEnum.USER_NOT_FOUND);
        });
    }

    public User getById(Long id) {
        return userRepository.findById(id).orElseThrow(() -> {
            log.warn("User with id: {} is not fount", id);
            return new AppException(AppErrorEnum.USER_NOT_FOUND);
        });
    }
}

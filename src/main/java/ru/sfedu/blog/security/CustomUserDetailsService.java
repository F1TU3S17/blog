package ru.sfedu.blog.security;

import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;
import ru.sfedu.blog.entity.User;
import ru.sfedu.blog.repository.UserRepositoryImplt;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {
    private final UserRepositoryImplt userRepositoryImplt;

    @Override
    public UserDetails loadUserByUsername(@NonNull String username)  {
        User user = userRepositoryImplt.getByLoginInput(username);
        return new CustomUserDetails(user);
    }
}

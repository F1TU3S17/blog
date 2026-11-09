package ru.sfedu.blog.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ru.sfedu.blog.dto.UserResponse;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users")
@Setter
@Getter
@NoArgsConstructor(force = true)
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String username;

    @Column(nullable = false)
    private String email;

    @Enumerated(EnumType.STRING)
    @NotNull
    private UserRoleEnum role;

    private String passwordHash;

    @OneToMany(mappedBy="author", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Post> posts = new ArrayList<>();

    public User(String username, String email,String passwordHash) {
        this.username = username;
        this.email = email;
        this.role = UserRoleEnum.USER;
        this.passwordHash = passwordHash;
    }

    public UserResponse toDto() {
        return new UserResponse(
                id,
                username,
                email,
                role,
                passwordHash
        );
    }
}

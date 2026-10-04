package com.library.service;

import com.library.model.User;
import com.library.repo.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
public class UserService implements UserDetailsService {

    private static final Pattern PASSWORD =
            Pattern.compile("^(?=.*\\p{Lu})(?=.*\\p{Ll})(?=.*\\d)(?=.*[^\\p{L}\\d]).+$");

    private final UserRepository users;
    private final PasswordEncoder encoder;

    public UserService(UserRepository users, PasswordEncoder encoder) {
        this.users = users;
        this.encoder = encoder;
    }

    @Override
    public UserDetails loadUserByUsername(String username) {
        User user = users.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException(username));
        return org.springframework.security.core.userdetails.User.withUsername(user.username)
                .password(user.passwordHash)
                .roles(user.role.name())
                .build();
    }

    @Transactional
    public void register(String username, String password) {
        if (username == null || username.isBlank() || password == null || password.length() < 10) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Логин и пароль (минимум 10 символов) обязательны");
        }
        if (!PASSWORD.matcher(password).matches()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Пароль должен содержать буквы верхнего и нижнего регистра, цифру и служебный символ");
        }
        if (users.existsByUsername(username)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Пользователь уже существует");
        }
        User user = new User();
        user.username = username.trim();
        user.passwordHash = encoder.encode(password);
        user.role = User.Role.USER;
        users.save(user);
    }

    @Transactional
    public void ensureModerator(String username, String password) {
        Optional<User> existing = users.findByUsername(username);
        if (existing.isPresent()) {
            return;
        }
        User user = new User();
        user.username = username;
        user.passwordHash = encoder.encode(password);
        user.role = User.Role.MODERATOR;
        users.save(user);
    }

    public Optional<User> current() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
            return Optional.empty();
        }
        return users.findByUsername(auth.getName());
    }

    public Map<String, Object> profile(String username) {
        User user = users.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException(username));
        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("username", user.username);
        profile.put("moderator", user.role == User.Role.MODERATOR);
        return profile;
    }
}

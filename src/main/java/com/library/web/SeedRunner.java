package com.library.web;

import com.library.service.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class SeedRunner implements CommandLineRunner {

    private final UserService users;
    private final String login;
    private final String password;

    public SeedRunner(UserService users,
                      @Value("${app.seed.moderator-login}") String login,
                      @Value("${app.seed.moderator-password}") String password) {
        this.users = users;
        this.login = login;
        this.password = password;
    }

    @Override
    public void run(String... args) {
        users.ensureModerator(login, password);
    }
}

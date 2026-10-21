package com.library.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.library.entity.Book;
import com.library.entity.Role;
import com.library.entity.User;
import com.library.repository.BookRepository;
import com.library.repository.UserRepository;

@Configuration
@Profile("dev")
public class DevelopmentData {
    @Bean
    CommandLineRunner seedDevelopmentData(
            UserRepository users,
            BookRepository books,
            PasswordEncoder passwordEncoder) {
        return args -> {
            users.findByEmail("student@library.local").orElseGet(() -> users.save(
                    new User("student@library.local", passwordEncoder.encode("library123"), Role.STUDENT)));
            users.findByEmail("admin@library.local").orElseGet(() -> users.save(
                    new User("admin@library.local", passwordEncoder.encode("library123"), Role.ADMIN)));
            if (books.count() == 0) {
                books.save(new Book("The Pragmatic Programmer", "David Thomas & Andrew Hunt",
                        "9780135957059", "TECHNOLOGY", 4));
                books.save(new Book("Clean Code", "Robert C. Martin",
                        "9780132350884", "TECHNOLOGY", 3));
                books.save(new Book("Atomic Habits", "James Clear",
                        "9780735211292", "PERSONAL DEVELOPMENT", 5));
            }
        };
    }
}

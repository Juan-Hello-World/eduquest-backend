package com.eduquest.api.repository;

import com.eduquest.api.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class UserRepositoryTest {

    @Container
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16")
            .withDatabaseName("eduquest_test")
            .withUsername("test_user")
            .withPassword("test_pass");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private UserRepository userRepository;

    @Test
    void deberiaGuardarYEncontrarUsuarioPorUsername() {
        User user = new User();
        user.setUsername("piero_test");
        user.setEmail("piero_test@utec.edu.pe");
        user.setPassword("hashedpassword123");

        userRepository.save(user);

        assertTrue(userRepository.existsByUsername("piero_test"));
        assertTrue(userRepository.existsByEmail("piero_test@utec.edu.pe"));

        Optional<User> found = userRepository.findByUsername("piero_test");
        assertTrue(found.isPresent());
        assertEquals("piero_test@utec.edu.pe", found.get().getEmail());
    }
}
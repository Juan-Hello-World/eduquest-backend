package com.eduquest.api.repository;

import com.eduquest.api.entity.PrivateGroup;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
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
class GroupRepositoryTest {

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
    private PrivateGroupRepository privateGroupRepository;

    @Test
    void deberiaGuardarYEncontrarGrupoPorId() {
        PrivateGroup group = new PrivateGroup();
        group.setName("Grupo de Estudio Backend");
        group.setDescription("Grupo para repasar Spring Boot");

        PrivateGroup guardado = privateGroupRepository.save(group);

        assertNotNull(guardado.getId());

        Optional<PrivateGroup> encontrado = privateGroupRepository.findById(guardado.getId());
        assertTrue(encontrado.isPresent());
        assertEquals("Grupo de Estudio Backend", encontrado.get().getName());
    }
}
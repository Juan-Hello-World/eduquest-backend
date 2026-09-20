package com.eduquest.api.repository;

import com.eduquest.api.entity.Document;
import com.eduquest.api.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class DocumentRepositoryTest {

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
    private DocumentRepository documentRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void deberiaEncontrarDocumentosPorTituloSinImportarMayusculas() {
        User user = new User();
        user.setUsername("piero_doc_test");
        user.setEmail("piero_doc_test@utec.edu.pe");
        user.setPassword("hashedpassword123");
        User usuarioGuardado = userRepository.save(user);

        Document document = new Document();
        document.setTitle("Guia de Spring Boot");
        document.setFileUrl("https://example.com/guia.pdf");
        document.setAuthor(usuarioGuardado);
        documentRepository.save(document);

        List<Document> encontrados = documentRepository.findByTitleContainingIgnoreCase("spring");

        assertEquals(1, encontrados.size());
        assertEquals("Guia de Spring Boot", encontrados.get(0).getTitle());
    }
}
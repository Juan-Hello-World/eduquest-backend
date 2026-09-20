package com.eduquest.api.repository;

import com.eduquest.api.entity.PrivateGroup;
import com.eduquest.api.entity.StudySession;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class StudySessionRepositoryTest {

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
    private StudySessionRepository studySessionRepository;

    @Autowired
    private PrivateGroupRepository privateGroupRepository;

    @Test
    void deberiaEncontrarSesionesPorGrupo() {
        PrivateGroup group = new PrivateGroup();
        group.setName("Grupo de Estudio Backend");
        group.setDescription("Grupo para repasar Spring Boot");
        PrivateGroup grupoGuardado = privateGroupRepository.save(group);

        StudySession session = new StudySession();
        session.setTitle("Repaso de Testcontainers");
        session.setDescription("Sesion para revisar tests de integracion");
        session.setScheduledAt(LocalDateTime.now().plusDays(7));
        session.setPrivateGroup(grupoGuardado);

        studySessionRepository.save(session);

        List<StudySession> sesiones = studySessionRepository.findByPrivateGroupId(grupoGuardado.getId());

        assertEquals(1, sesiones.size());
        assertEquals("Repaso de Testcontainers", sesiones.get(0).getTitle());
    }
}
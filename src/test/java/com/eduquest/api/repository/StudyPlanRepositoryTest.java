package com.eduquest.api.repository;

import com.eduquest.api.entity.StudyPlan;
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
class StudyPlanRepositoryTest {

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
    private StudyPlanRepository studyPlanRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void deberiaEncontrarPlanesPorUsuario() {
        User user = new User();
        user.setUsername("piero_plan_test");
        user.setEmail("piero_plan_test@utec.edu.pe");
        user.setPassword("hashedpassword123");
        User usuarioGuardado = userRepository.save(user);

        StudyPlan plan = new StudyPlan();
        plan.setTitle("Plan de Spring Boot");
        plan.setCourseName("Desarrollo Basado en Plataformas");
        plan.setDifficulty("Media");
        plan.setGeneratedContent("Contenido generado de prueba");
        plan.setUser(usuarioGuardado);
        studyPlanRepository.save(plan);

        List<StudyPlan> planes = studyPlanRepository.findByUserId(usuarioGuardado.getId());

        assertEquals(1, planes.size());
        assertEquals("Plan de Spring Boot", planes.get(0).getTitle());
    }
}
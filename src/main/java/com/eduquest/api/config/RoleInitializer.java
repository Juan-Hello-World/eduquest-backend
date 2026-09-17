package com.eduquest.api.config;

import com.eduquest.api.entity.Role;
import com.eduquest.api.repository.RoleRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class RoleInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;

    public RoleInitializer(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        // Solo inserta los roles si la tabla está vacía
        if (roleRepository.count() == 0) {
            roleRepository.save(new Role(null, Role.ERole.ROLE_USER));
            roleRepository.save(new Role(null, Role.ERole.ROLE_ADMIN));
            roleRepository.save(new Role(null, Role.ERole.ROLE_ASSISTANT));
            System.out.println("Roles inicializados por defecto en la base de datos.");
        }
    }
}
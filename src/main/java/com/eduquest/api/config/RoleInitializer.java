package com.eduquest.api.config;

import com.eduquest.api.entity.Role;
import com.eduquest.api.entity.RoleName;
import com.eduquest.api.entity.User;
import com.eduquest.api.repository.RoleRepository;
import com.eduquest.api.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Collections;

@Component
public class RoleInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(RoleInitializer.class);

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public RoleInitializer(RoleRepository roleRepository, UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        seedRole(RoleName.ROLE_USER);
        seedRole(RoleName.ROLE_ADMIN);
        seedRole(RoleName.ROLE_MANAGER);

        if (!userRepository.existsByUsername("admin")) {
            Role adminRole = roleRepository.findByName(RoleName.ROLE_ADMIN).orElseThrow();
            User admin = new User();
            admin.setUsername("admin");
            admin.setEmail("admin@utec.edu.pe");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setRoles(Collections.singleton(adminRole));
            userRepository.save(admin);
            log.info("Usuario admin creado: admin@utec.edu.pe / admin123");
        }
    }

    private void seedRole(RoleName name) {
        if (roleRepository.findByName(name).isEmpty()) {
            roleRepository.save(new Role(null, name));
            log.info("Rol creado: {}", name);
        }
    }
}
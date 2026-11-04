package com.eduquest.api.config;

import com.eduquest.api.entity.Role;
import com.eduquest.api.entity.RoleName;
import com.eduquest.api.entity.User;
import com.eduquest.api.repository.RoleRepository;
import com.eduquest.api.repository.UserRepository;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Component
public class RoleInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${admin.initial.password:}")
    private String adminInitialPassword;

    public RoleInitializer(RoleRepository roleRepository,
                           UserRepository userRepository,
                           PasswordEncoder passwordEncoder) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String @NonNull ... args) {
        createRoleIfNotFound(RoleName.ROLE_USER);
        createRoleIfNotFound(RoleName.ROLE_MANAGER);
        Role adminRole = createRoleIfNotFound(RoleName.ROLE_ADMIN);

        // Solo crea el usuario admin inicial si se proveyó una contraseña por configuración/variable de entorno
        if (adminInitialPassword != null && !adminInitialPassword.isBlank()
                && userRepository.findByUsername("admin").isEmpty()) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setEmail("admin@eduquest.com");
            admin.setPassword(passwordEncoder.encode(adminInitialPassword));
            admin.setRoles(Set.of(adminRole));
            userRepository.save(admin);
        }
    }

    private Role createRoleIfNotFound(RoleName name) {
        return roleRepository.findByName(name).orElseGet(() -> {
            Role role = new Role();
            role.setName(name);
            return roleRepository.save(role);
        });
    }
}
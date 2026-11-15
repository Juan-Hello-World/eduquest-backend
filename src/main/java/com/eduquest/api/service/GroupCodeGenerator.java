package com.eduquest.api.service;

import com.eduquest.api.repository.PrivateGroupRepository;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Base64;

@Component
public class GroupCodeGenerator {

    private static final String ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
    private static final int LENGTH = 6;

    private final SecureRandom random = new SecureRandom();
    private final PrivateGroupRepository groupRepository;

    public GroupCodeGenerator(PrivateGroupRepository groupRepository) {
        this.groupRepository = groupRepository;
    }

    public String nextCode() {
        String code;
        do {
            StringBuilder builder = new StringBuilder(LENGTH);
            for (int i = 0; i < LENGTH; i++) {
                builder.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
            }
            code = builder.toString();
        } while (groupRepository.existsByCode(code));
        return code;
    }

    public String nextInviteToken() {
        byte[] bytes = new byte[18];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}

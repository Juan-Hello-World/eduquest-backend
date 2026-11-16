package com.eduquest.api.service.impl;

import com.eduquest.api.entity.PushToken;
import com.eduquest.api.entity.User;
import com.eduquest.api.event.OnPushEvent;
import com.eduquest.api.exception.ResourceNotFoundException;
import com.eduquest.api.repository.PushTokenRepository;
import com.eduquest.api.repository.UserRepository;
import com.eduquest.api.service.PushNotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ExpoPushNotificationService implements PushNotificationService {

    private static final Logger log = LoggerFactory.getLogger(ExpoPushNotificationService.class);
    private static final int BATCH_SIZE = 100;

    private final PushTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final RestTemplate restTemplate;
    private final String endpoint;

    public ExpoPushNotificationService(PushTokenRepository tokenRepository,
                                       UserRepository userRepository,
                                       RestTemplate restTemplate,
                                       @Value("${push.expo.url:https://exp.host/--/api/v2/push/send}") String endpoint) {
        this.tokenRepository = tokenRepository;
        this.userRepository = userRepository;
        this.restTemplate = restTemplate;
        this.endpoint = endpoint;
    }

    @Override
    @Transactional
    public void register(String username, String token) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        PushToken pushToken = tokenRepository.findByToken(token).orElseGet(PushToken::new);
        pushToken.setToken(token);
        pushToken.setUser(user);
        pushToken.setCreatedAt(LocalDateTime.now());
        tokenRepository.save(pushToken);
    }

    @Override
    @Transactional
    public void unregister(String username, String token) {
        userRepository.findByUsername(username)
                .ifPresent(user -> tokenRepository.deleteByTokenAndUserId(token, user.getId()));
    }

    @Override
    @Transactional
    public void send(List<OnPushEvent.PushMessage> messages) {
        if (messages.isEmpty()) {
            return;
        }
        Set<Long> userIds = messages.stream().map(OnPushEvent.PushMessage::userId).collect(Collectors.toSet());
        Map<Long, List<String>> tokensByUser = tokenRepository.findByUserIdIn(userIds).stream()
                .collect(Collectors.groupingBy(token -> token.getUser().getId(),
                        Collectors.mapping(PushToken::getToken, Collectors.toList())));
        if (tokensByUser.isEmpty()) {
            return;
        }

        List<Map<String, Object>> payload = new ArrayList<>();
        for (OnPushEvent.PushMessage message : messages) {
            for (String token : tokensByUser.getOrDefault(message.userId(), List.of())) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("to", token);
                item.put("title", message.title());
                item.put("body", message.body());
                item.put("sound", "default");
                item.put("data", message.link() == null ? Map.of() : Map.of("link", message.link()));
                payload.add(item);
            }
        }

        Set<String> invalid = new HashSet<>();
        for (int start = 0; start < payload.size(); start += BATCH_SIZE) {
            List<Map<String, Object>> batch = payload.subList(start, Math.min(payload.size(), start + BATCH_SIZE));
            invalid.addAll(post(batch));
        }
        if (!invalid.isEmpty()) {
            tokenRepository.deleteByTokenIn(invalid);
        }
    }

    @SuppressWarnings("unchecked")
    private Set<String> post(List<Map<String, Object>> batch) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        Set<String> invalid = new HashSet<>();
        try {
            Map<String, Object> response = restTemplate.postForObject(endpoint, new HttpEntity<>(batch, headers), Map.class);
            Object data = response == null ? null : response.get("data");
            if (data instanceof List<?> tickets) {
                for (int index = 0; index < tickets.size() && index < batch.size(); index++) {
                    if (tickets.get(index) instanceof Map<?, ?> ticket
                            && "error".equals(ticket.get("status"))
                            && ticket.get("details") instanceof Map<?, ?> details
                            && "DeviceNotRegistered".equals(details.get("error"))) {
                        invalid.add(String.valueOf(batch.get(index).get("to")));
                    }
                }
            }
        } catch (RestClientException ex) {
            log.warn("No se pudo enviar la notificación push: {}", ex.getMessage());
        }
        return invalid;
    }
}

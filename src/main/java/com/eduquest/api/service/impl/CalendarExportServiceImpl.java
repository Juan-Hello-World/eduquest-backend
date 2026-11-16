package com.eduquest.api.service.impl;

import com.eduquest.api.entity.StudySession;
import com.eduquest.api.entity.User;
import com.eduquest.api.exception.ResourceNotFoundException;
import com.eduquest.api.repository.StudySessionRepository;
import com.eduquest.api.repository.UserRepository;
import com.eduquest.api.service.CalendarExportService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@Service
public class CalendarExportServiceImpl implements CalendarExportService {

    private static final DateTimeFormatter ICS_TIME = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'");
    private static final ZoneId ZONE = ZoneId.systemDefault();
    private static final int DEFAULT_DURATION_MINUTES = 60;

    private final StudySessionRepository sessionRepository;
    private final UserRepository userRepository;

    public CalendarExportServiceImpl(StudySessionRepository sessionRepository, UserRepository userRepository) {
        this.sessionRepository = sessionRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public String exportMySessions(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        LocalDateTime now = LocalDateTime.now();
        StringBuilder ics = new StringBuilder();
        line(ics, "BEGIN:VCALENDAR");
        line(ics, "VERSION:2.0");
        line(ics, "PRODID:-//EduQuest//Asesorias//ES");
        line(ics, "CALSCALE:GREGORIAN");
        line(ics, "METHOD:PUBLISH");
        line(ics, "X-WR-CALNAME:EduQuest - Mis asesorías");
        for (StudySession session : sessionRepository.findForMemberBetween(user.getId(), now.minusDays(30), now.plusDays(180))) {
            LocalDateTime end = session.getEndsAt() != null ? session.getEndsAt()
                    : session.getScheduledAt().plusMinutes(DEFAULT_DURATION_MINUTES);
            line(ics, "BEGIN:VEVENT");
            line(ics, "UID:session-" + session.getId() + "@eduquest");
            line(ics, "DTSTAMP:" + utc(now));
            line(ics, "DTSTART:" + utc(session.getScheduledAt()));
            line(ics, "DTEND:" + utc(end));
            line(ics, "SUMMARY:" + escape(session.getTitle() + " · " + session.getPrivateGroup().getName()));
            StringBuilder description = new StringBuilder();
            if (session.getDescription() != null && !session.getDescription().isBlank()) {
                description.append(session.getDescription().trim()).append("\n\n");
            }
            if (session.getMeetingUrl() != null) {
                description.append("Enlace: ").append(session.getMeetingUrl());
                line(ics, "URL:" + session.getMeetingUrl());
            }
            line(ics, "DESCRIPTION:" + escape(description.toString()));
            line(ics, "BEGIN:VALARM");
            line(ics, "TRIGGER:-PT30M");
            line(ics, "ACTION:DISPLAY");
            line(ics, "DESCRIPTION:" + escape(session.getTitle()));
            line(ics, "END:VALARM");
            line(ics, "END:VEVENT");
        }
        line(ics, "END:VCALENDAR");
        return ics.toString();
    }

    private static String utc(LocalDateTime value) {
        return value.atZone(ZONE).withZoneSameInstant(ZoneOffset.UTC).format(ICS_TIME);
    }

    static String escape(String value) {
        return value.replace("\\", "\\\\").replace(";", "\\;").replace(",", "\\,").replace("\n", "\\n");
    }

    private static void line(StringBuilder ics, String content) {
        String remaining = content;
        boolean first = true;
        while (remaining.length() > 74) {
            ics.append(first ? "" : " ").append(remaining, 0, 74).append("\r\n");
            remaining = remaining.substring(74);
            first = false;
        }
        ics.append(first ? "" : " ").append(remaining).append("\r\n");
    }
}

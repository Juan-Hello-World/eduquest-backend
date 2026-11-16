package com.eduquest.api.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import com.eduquest.api.exception.UserBannedException;

import java.io.IOException;

@Component
public class JwtAuthorizationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsServiceImpl customUserDetailsService;

    public JwtAuthorizationFilter(JwtService jwtService, UserDetailsServiceImpl customUserDetailsService) {
        this.jwtService = jwtService;
        this.customUserDetailsService = customUserDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        final String authHeader = request.getHeader("Authorization");

        if (StringUtils.hasText(authHeader) && StringUtils.startsWithIgnoreCase(authHeader, "Bearer ")) {
            String token = authHeader.substring(7);

            if (jwtService.isValidToken(token) && "access".equals(jwtService.getTokenType(token))) {
                String username = jwtService.extractUsername(token);

                if (StringUtils.hasText(username) && SecurityContextHolder.getContext().getAuthentication() == null) {
                    UserDetails userDetails = customUserDetailsService.loadUserByUsername(username);
                    if (userDetails instanceof UserPrincipal principal && principal.isBanned()) {
                        writeBannedResponse(request, response, principal);
                        return;
                    }
                    if (userDetails instanceof UserPrincipal principal && !principal.isEmailVerified()
                            && requiresVerifiedEmail(request)) {
                        writeUnverifiedResponse(request, response);
                        return;
                    }
                    SecurityContext context = SecurityContextHolder.createEmptyContext();
                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    context.setAuthentication(authToken);
                    SecurityContextHolder.setContext(context);
                }
            }
        }

        filterChain.doFilter(request, response);
    }

    static boolean requiresVerifiedEmail(HttpServletRequest request) {
        String method = request.getMethod();
        if ("GET".equals(method) || "HEAD".equals(method) || "OPTIONS".equals(method)) {
            return false;
        }
        String path = request.getRequestURI();
        return !(path.startsWith("/api/v1/auth/")
                || path.startsWith("/api/v1/users/me")
                || path.startsWith("/api/v1/notifications")
                || path.endsWith("/seen")
                || path.endsWith("/read"));
    }

    private void writeUnverifiedResponse(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String message = "Verifica tu correo para poder publicar y escribir en EduQuest";
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/problem+json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{"
                + "\"status\":403,"
                + "\"title\":\"Forbidden\","
                + "\"code\":\"EMAIL_NOT_VERIFIED\","
                + "\"message\":" + json(message) + ","
                + "\"detail\":" + json(message) + ","
                + "\"path\":" + json(request.getRequestURI())
                + "}");
    }

    private void writeBannedResponse(HttpServletRequest request, HttpServletResponse response,
                                     UserPrincipal principal) throws IOException {
        String message = "Tu cuenta fue suspendida por incumplir las normas de EduQuest";
        String bannedAt = principal.getBannedAt() != null ? principal.getBannedAt().toString() : null;
        String bannedUntil = principal.getBannedUntil() != null ? principal.getBannedUntil().toString() : null;
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/problem+json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{"
                + "\"status\":403,"
                + "\"title\":\"Forbidden\","
                + "\"code\":\"" + UserBannedException.CODE + "\","
                + "\"message\":" + json(message) + ","
                + "\"detail\":" + json(message) + ","
                + "\"banReason\":" + json(principal.getBanReason()) + ","
                + "\"bannedAt\":" + json(bannedAt) + ","
                + "\"bannedUntil\":" + json(bannedUntil) + ","
                + "\"path\":" + json(request.getRequestURI())
                + "}");
    }

    private static String json(String value) {
        if (value == null) {
            return "null";
        }
        StringBuilder builder = new StringBuilder("\"");
        for (char c : value.toCharArray()) {
            switch (c) {
                case '"' -> builder.append("\\\"");
                case '\\' -> builder.append("\\\\");
                case '\n' -> builder.append("\\n");
                case '\r' -> builder.append("\\r");
                case '\t' -> builder.append("\\t");
                default -> {
                    if (c < 0x20) {
                        builder.append(String.format("\\u%04x", (int) c));
                    } else {
                        builder.append(c);
                    }
                }
            }
        }
        return builder.append('"').toString();
    }
}
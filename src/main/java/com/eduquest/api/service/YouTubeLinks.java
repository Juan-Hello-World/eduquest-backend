package com.eduquest.api.service;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class YouTubeLinks {

    private static final Pattern VIDEO_URL = Pattern.compile(
            "^https?://(?:www\\.|m\\.)?(?:youtube\\.com/(?:watch\\?(?:.*&)?v=|live/|shorts/|embed/)|youtu\\.be/)"
                    + "([A-Za-z0-9_-]{11})(?:[?&#/].*)?$");

    private YouTubeLinks() {
    }

    public static Optional<String> extractVideoId(String url) {
        if (url == null) {
            return Optional.empty();
        }
        Matcher matcher = VIDEO_URL.matcher(url.trim());
        return matcher.matches() ? Optional.of(matcher.group(1)) : Optional.empty();
    }
}

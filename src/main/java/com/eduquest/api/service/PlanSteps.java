package com.eduquest.api.service;

import java.util.regex.Pattern;

public final class PlanSteps {

    private static final Pattern STEP = Pattern.compile("^(?:[-*•]|\\d+[.)])\\s+\\S.*$");

    private PlanSteps() {
    }

    public static int count(String content) {
        if (content == null || content.isBlank()) {
            return 0;
        }
        return (int) content.lines()
                .map(String::trim)
                .filter(line -> STEP.matcher(line).matches())
                .count();
    }
}

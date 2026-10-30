package com.mtmrails.esscmd.util;

import java.time.Duration;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Essentials-style durations such as {@code 1d2h30m} or {@code 2w}. Units: {@code y} (365 days),
 * {@code mo} (30 days), {@code w}, {@code d}, {@code h}, {@code m}, {@code s}. Each unit may
 * appear once, largest first; spaces between parts are allowed.
 */
public final class DateDiff {
    private static final Pattern FORMAT = Pattern.compile(
            "(?:(\\d+)\\s*y)?\\s*(?:(\\d+)\\s*mo)?\\s*(?:(\\d+)\\s*w)?\\s*(?:(\\d+)\\s*d)?\\s*"
                    + "(?:(\\d+)\\s*h)?\\s*(?:(\\d+)\\s*m)?\\s*(?:(\\d+)\\s*s)?");
    private static final long[] UNIT_SECONDS = {365L * 86400, 30L * 86400, 7L * 86400, 86400, 3600, 60, 1};
    private static final String[] UNIT_NAMES = {"y", "mo", "w", "d", "h", "m", "s"};

    private DateDiff() {}

    /** The duration, or empty if {@code text} isn't a valid non-zero duration. */
    public static Optional<Duration> parse(String text) {
        String trimmed = text.trim().toLowerCase(Locale.ROOT);
        if (trimmed.isEmpty()) {
            return Optional.empty();
        }
        Matcher m = FORMAT.matcher(trimmed);
        if (!m.matches()) {
            return Optional.empty();
        }
        long seconds = 0;
        try {
            for (int i = 0; i < UNIT_SECONDS.length; i++) {
                String group = m.group(i + 1);
                if (group != null) {
                    seconds = Math.addExact(seconds, Math.multiplyExact(Long.parseLong(group), UNIT_SECONDS[i]));
                }
            }
        } catch (ArithmeticException | NumberFormatException e) {
            return Optional.empty();
        }
        return seconds > 0 ? Optional.of(Duration.ofSeconds(seconds)) : Optional.empty();
    }

    /** Compact form for messages: {@code 1d 2h 30m}. Durations under a second are {@code 0s}. */
    public static String format(Duration duration) {
        long seconds = Math.max(0, duration.getSeconds());
        if (seconds == 0) {
            return "0s";
        }
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < UNIT_SECONDS.length; i++) {
            long count = seconds / UNIT_SECONDS[i];
            if (count > 0) {
                if (out.length() > 0) {
                    out.append(' ');
                }
                out.append(count).append(UNIT_NAMES[i]);
                seconds -= count * UNIT_SECONDS[i];
            }
        }
        return out.toString();
    }
}

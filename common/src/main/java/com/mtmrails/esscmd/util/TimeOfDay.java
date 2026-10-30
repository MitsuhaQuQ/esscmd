package com.mtmrails.esscmd.util;

import java.util.Locale;
import java.util.Map;
import java.util.OptionalLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Essentials time arguments in ticks: {@code day}, {@code 4pm}, {@code 17:30}, {@code 4000ticks}. */
public final class TimeOfDay {
    private static final Map<String, Long> NAMES = Map.of("sunrise", 23000L, "dawn", 23000L, "day", 1000L,
            "morning", 1000L, "noon", 6000L, "midday", 6000L, "sunset", 12000L, "dusk", 12000L,
            "night", 13000L, "midnight", 18000L);
    private static final Pattern TICKS = Pattern.compile("(\\d+)(?:t|ticks)?");
    private static final Pattern CLOCK = Pattern.compile("(\\d{1,2}):(\\d{2})");
    private static final Pattern AM_PM = Pattern.compile("(\\d{1,2})(am|pm)");

    private TimeOfDay() {}

    public static OptionalLong parse(String text) {
        String value = text.toLowerCase(Locale.ROOT);
        Long named = NAMES.get(value);
        if (named != null) {
            return OptionalLong.of(named);
        }
        Matcher clock = CLOCK.matcher(value);
        if (clock.matches()) {
            int hour = Integer.parseInt(clock.group(1));
            int minute = Integer.parseInt(clock.group(2));
            return hour < 24 && minute < 60 ? OptionalLong.of(fromClock(hour, minute)) : OptionalLong.empty();
        }
        Matcher amPm = AM_PM.matcher(value);
        if (amPm.matches()) {
            int hour = Integer.parseInt(amPm.group(1));
            if (hour < 1 || hour > 12) {
                return OptionalLong.empty();
            }
            return OptionalLong.of(fromClock(hour % 12 + (amPm.group(2).equals("pm") ? 12 : 0), 0));
        }
        Matcher ticks = TICKS.matcher(value);
        if (ticks.matches() && ticks.group(1).length() <= 9) {
            return OptionalLong.of(Long.parseLong(ticks.group(1)) % 24000);
        }
        return OptionalLong.empty();
    }

    /** Tick 0 is 06:00. */
    private static long fromClock(int hour, int minute) {
        return Math.floorMod((hour - 6) * 1000L + minute * 1000L / 60, 24000L);
    }
}

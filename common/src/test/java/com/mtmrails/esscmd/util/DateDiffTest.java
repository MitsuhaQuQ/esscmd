package com.mtmrails.esscmd.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class DateDiffTest {
    @Test
    void parsesSingleUnits() {
        assertEquals(Duration.ofSeconds(30), DateDiff.parse("30s").orElseThrow());
        assertEquals(Duration.ofMinutes(5), DateDiff.parse("5m").orElseThrow());
        assertEquals(Duration.ofHours(2), DateDiff.parse("2h").orElseThrow());
        assertEquals(Duration.ofDays(1), DateDiff.parse("1d").orElseThrow());
        assertEquals(Duration.ofDays(14), DateDiff.parse("2w").orElseThrow());
        assertEquals(Duration.ofDays(30), DateDiff.parse("1mo").orElseThrow());
        assertEquals(Duration.ofDays(365), DateDiff.parse("1y").orElseThrow());
    }

    @Test
    void parsesCombinationsSpacesAndCase() {
        assertEquals(Duration.ofDays(1).plusHours(2).plusMinutes(30), DateDiff.parse("1d2h30m").orElseThrow());
        assertEquals(Duration.ofDays(1).plusHours(2).plusMinutes(30), DateDiff.parse(" 1d 2h 30m ").orElseThrow());
        assertEquals(Duration.ofDays(30).plusMinutes(1), DateDiff.parse("1MO1M").orElseThrow());
    }

    @Test
    void rejectsInvalidInput() {
        for (String bad : new String[] {"", "  ", "30", "abc", "1x", "5m1h", "0s", "1d1d", "-5m", "99999999999999999999y"}) {
            assertTrue(DateDiff.parse(bad).isEmpty(), bad);
        }
    }

    @Test
    void formatsLargestUnitsFirst() {
        assertEquals("1d 2h 30m", DateDiff.format(Duration.ofDays(1).plusHours(2).plusMinutes(30)));
        assertEquals("2w 3s", DateDiff.format(Duration.ofDays(14).plusSeconds(3)));
        assertEquals("0s", DateDiff.format(Duration.ZERO));
        assertEquals("0s", DateDiff.format(Duration.ofSeconds(-5)));
    }

    @Test
    void formatRoundTrips() {
        Duration d = Duration.ofDays(400).plusHours(5).plusSeconds(9);
        assertEquals(d, DateDiff.parse(DateDiff.format(d)).orElseThrow());
    }
}

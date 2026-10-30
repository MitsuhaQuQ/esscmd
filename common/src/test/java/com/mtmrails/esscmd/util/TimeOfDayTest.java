package com.mtmrails.esscmd.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TimeOfDayTest {
    @Test
    void parsesNamesClockAndTicks() {
        assertEquals(1000, TimeOfDay.parse("day").orElseThrow());
        assertEquals(18000, TimeOfDay.parse("MIDNIGHT").orElseThrow());
        assertEquals(0, TimeOfDay.parse("6:00").orElseThrow());
        assertEquals(11500, TimeOfDay.parse("17:30").orElseThrow());
        assertEquals(22000, TimeOfDay.parse("4:00").orElseThrow());
        assertEquals(10000, TimeOfDay.parse("4pm").orElseThrow());
        assertEquals(18000, TimeOfDay.parse("12am").orElseThrow());
        assertEquals(6000, TimeOfDay.parse("12pm").orElseThrow());
        assertEquals(4000, TimeOfDay.parse("4000ticks").orElseThrow());
        assertEquals(4000, TimeOfDay.parse("28000").orElseThrow());
    }

    @Test
    void rejectsInvalidInput() {
        for (String bad : new String[] {"", "abc", "25:00", "12:60", "13pm", "0am"}) {
            assertTrue(TimeOfDay.parse(bad).isEmpty(), bad);
        }
    }
}

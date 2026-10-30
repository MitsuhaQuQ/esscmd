package com.mtmrails.esscmd.command;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class CompassTest {
    @Test
    void convertsMinecraftYawToBearing() {
        // Minecraft: yaw 0 faces south, 90 west, 180/-180 north, -90 east.
        assertEquals(180, LocationInfoCommands.bearingDegrees(0));
        assertEquals(270, LocationInfoCommands.bearingDegrees(90));
        assertEquals(0, LocationInfoCommands.bearingDegrees(180));
        assertEquals(0, LocationInfoCommands.bearingDegrees(-180));
        assertEquals(90, LocationInfoCommands.bearingDegrees(-90));
        assertEquals(90, LocationInfoCommands.bearingDegrees(270));
    }

    @Test
    void namesEightPoints() {
        assertEquals("N", LocationInfoCommands.bearing(0));
        assertEquals("N", LocationInfoCommands.bearing(350));
        assertEquals("NE", LocationInfoCommands.bearing(45));
        assertEquals("E", LocationInfoCommands.bearing(90));
        assertEquals("S", LocationInfoCommands.bearing(180));
        assertEquals("W", LocationInfoCommands.bearing(270));
        assertEquals("NW", LocationInfoCommands.bearing(315));
    }
}

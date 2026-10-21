package com.reminder.util;

import org.junit.jupiter.api.Test;

import java.util.OptionalInt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HoursTest {

    @Test
    void entryHoursRange() {
        assertTrue(Hours.validEntry(1));
        assertTrue(Hours.validEntry(8.5));
        assertTrue(Hours.validEntry(24));
        assertFalse(Hours.validEntry(0));
        assertFalse(Hours.validEntry(-1));
        assertFalse(Hours.validEntry(25));
    }

    @Test
    void weekCellRange() {
        assertTrue(Hours.validWeekCell(0));
        assertTrue(Hours.validWeekCell(24));
        assertFalse(Hours.validWeekCell(-1));
        assertFalse(Hours.validWeekCell(25));
    }

    @Test
    void parsesWeekCell() {
        assertEquals(OptionalInt.of(0), Hours.parseWeekCell(""));
        assertEquals(OptionalInt.of(0), Hours.parseWeekCell("  "));
        assertEquals(OptionalInt.of(24), Hours.parseWeekCell(" 24 "));
        assertEquals(OptionalInt.empty(), Hours.parseWeekCell("-1"));
        assertEquals(OptionalInt.empty(), Hours.parseWeekCell("25"));
        assertEquals(OptionalInt.empty(), Hours.parseWeekCell("abc"));
    }
}

package com.reminder.settings;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DecimalSeparatorTest {

    @Test
    void dotFormatsWithDot() {
        assertEquals("7.5", DecimalSeparator.DOT.format(7.5, 1));
    }

    @Test
    void commaFormatsWithComma() {
        assertEquals("7,5", DecimalSeparator.COMMA.format(7.5, 1));
    }

    @Test
    void fromKeyOrDefault() {
        assertEquals(DecimalSeparator.COMMA, DecimalSeparator.fromKeyOrDefault("comma"));
        assertEquals(DecimalSeparator.DOT, DecimalSeparator.fromKeyOrDefault("dot"));
        assertEquals(DecimalSeparator.DOT, DecimalSeparator.fromKeyOrDefault(null));
        assertEquals(DecimalSeparator.DOT, DecimalSeparator.fromKeyOrDefault("unknown"));
    }
}

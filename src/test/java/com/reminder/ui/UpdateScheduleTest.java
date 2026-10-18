package com.reminder.ui;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UpdateScheduleTest {

    @Test
    void beforeTargetSameDay() {
        LocalDateTime now = LocalDateTime.of(2026, 10, 18, 1, 0);
        assertEquals(Duration.ofHours(1).toMillis(), UpdateCheckUi.initialDelayMillis(now, 2, 0));
    }

    @Test
    void afterTargetNextDay() {
        LocalDateTime now = LocalDateTime.of(2026, 10, 18, 3, 30);
        assertEquals(Duration.ofHours(22).plusMinutes(30).toMillis(),
                UpdateCheckUi.initialDelayMillis(now, 2, 0));
    }

    @Test
    void exactlyAtTargetGoesToNextDay() {
        LocalDateTime now = LocalDateTime.of(2026, 10, 18, 2, 0);
        assertEquals(Duration.ofDays(1).toMillis(), UpdateCheckUi.initialDelayMillis(now, 2, 0));
    }
}

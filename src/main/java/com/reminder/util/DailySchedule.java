package com.reminder.util;

import java.time.Duration;
import java.time.LocalDateTime;

/** Расчёт задержки до ежедневного времени HH:MM. */
public final class DailySchedule {

    private DailySchedule() {
    }

    /** Миллисекунды до следующего срабатывания в {@code hour:minute} (сегодня или завтра). */
    public static long initialDelayMillis(LocalDateTime now, int hour, int minute) {
        LocalDateTime next = now.toLocalDate().atTime(hour, minute);
        if (!next.isAfter(now)) {
            next = next.plusDays(1);
        }
        return Duration.between(now, next).toMillis();
    }
}

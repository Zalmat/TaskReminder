package com.reminder.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class Reminder implements Serializable {
    private static final long serialVersionUID = 1L;
    private String id;
    private LocalTime time;
    private String text;
    private boolean active;
    private boolean repeatDaily;
    private LocalDateTime snoozeUntil;

    public Reminder() {
        this.id = UUID.randomUUID().toString();
        this.active = true;
        this.repeatDaily = false;
    }

    public Reminder(LocalTime time, String text) {
        this();
        this.time = time;
        this.text = text;
    }

    public Reminder(LocalTime time, String text, boolean repeatDaily) {
        this(time, text);
        this.repeatDaily = repeatDaily;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public LocalTime getTime() { return time; }
    public void setTime(LocalTime time) { this.time = time; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public boolean isRepeatDaily() { return repeatDaily; }
    public void setRepeatDaily(boolean repeatDaily) { this.repeatDaily = repeatDaily; }

    public LocalDateTime getSnoozeUntil() { return snoozeUntil; }
    public void setSnoozeUntil(LocalDateTime snoozeUntil) { this.snoozeUntil = snoozeUntil; }

    /**
     * Напоминание срабатывает, если активно, не отложено и наступил его минутный интервал.
     * Окно срабатывания — одна минута [time, time+1min).
     */
    public boolean shouldTrigger() {
        if (!active) return false;
        if (snoozeUntil != null && LocalDateTime.now().isBefore(snoozeUntil)) {
            return false;
        }
        LocalTime now = LocalTime.now();
        int nowSec = now.toSecondOfDay();
        int targetSec = time.toSecondOfDay();
        return nowSec >= targetSec && nowSec < targetSec + 60;
    }

    public String getFormattedTime() {
        return time != null ? time.format(DateTimeFormatter.ofPattern("HH:mm")) : "";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Reminder reminder = (Reminder) o;
        return id != null && id.equals(reminder.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}
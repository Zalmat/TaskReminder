package com.reminder.models;

import java.io.Serializable;
import java.time.LocalTime;
import java.time.LocalDateTime;

public class Reminder implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private LocalTime time;
    private String text;
    private boolean active;
    private LocalDateTime snoozeUntil;
    private boolean repeatDaily;

    public Reminder() {
        this.id = java.util.UUID.randomUUID().toString();
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

    // Геттеры и сеттеры
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public LocalTime getTime() { return time; }
    public void setTime(LocalTime time) { this.time = time; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public LocalDateTime getSnoozeUntil() { return snoozeUntil; }
    public void setSnoozeUntil(LocalDateTime snoozeUntil) { this.snoozeUntil = snoozeUntil; }

    public boolean isRepeatDaily() { return repeatDaily; }
    public void setRepeatDaily(boolean repeatDaily) { this.repeatDaily = repeatDaily; }

    public boolean shouldTrigger() {
        if (!active) return false;
        if (snoozeUntil != null && LocalDateTime.now().isBefore(snoozeUntil)) {
            return false;
        }
        LocalTime now = LocalTime.now();
        // Проверяем с точностью до минуты
        return Math.abs(now.toSecondOfDay() - time.toSecondOfDay()) < 60;
    }

    public String getFormattedTime() {
        return time.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"));
    }
}
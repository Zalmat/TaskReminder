package com.reminder.model;

import com.reminder.util.DateUtils;
import com.reminder.util.RussianDays;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class WorkEntry implements Serializable {
    private static final long serialVersionUID = 1L;
    private LocalDate date;
    private String project;
    private String taskName;
    private String type;
    private double hours;
    private String comment;
    private LocalDateTime createdAt;

    public WorkEntry() {
        this.date = LocalDate.now();
        this.createdAt = LocalDateTime.now();
        this.comment = "";
        this.hours = 0;
    }

    public WorkEntry(String project, String taskName, String type, double hours) {
        this();
        this.project = project;
        this.taskName = taskName;
        this.type = type;
        this.hours = hours;
    }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public String getProject() { return project; }

    public String getTaskName() { return taskName; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public double getHours() { return hours; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    /** Русское название дня недели для отчётов (например «Понедельник»). */
    public String getDayOfWeek() {
        return RussianDays.fullName(date.getDayOfWeek());
    }

    public String getFormattedDate() {
        return DateUtils.formatDate(date);
    }

    @Override
    public String toString() {
        return String.format("[%s] %s - %s: %.1f ч",
                getFormattedDate(), project, taskName, hours);
    }
}
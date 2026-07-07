package com.reminder.models;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

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

    public WorkEntry(Task task) {
        this();
        this.project = task.getProject();
        this.taskName = task.getTaskName();
        this.type = task.getType();
        this.hours = task.getHours();
        this.comment = task.getComment();
    }

    // Геттеры и сеттеры
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public String getProject() { return project; }
    public void setProject(String project) { this.project = project; }

    public String getTaskName() { return taskName; }
    public void setTaskName(String taskName) { this.taskName = taskName; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public double getHours() { return hours; }
    public void setHours(double hours) { this.hours = hours; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getDayOfWeek() {
        return date.getDayOfWeek().toString();
    }

    public String getFormattedDate() {
        return date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
    }

    @Override
    public String toString() {
        return String.format("[%s] %s - %s: %.1f ч",
                getFormattedDate(), project, taskName, hours);
    }
}
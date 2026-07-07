package com.reminder.models;

import java.io.Serializable;

public class Task implements Serializable {
    private static final long serialVersionUID = 1L;

    private String project;
    private String taskName;
    private String type;
    private double hours;
    private String comment;

    public Task() {
        this.hours = 0;
        this.comment = "";
    }

    public Task(String project, String taskName, String type) {
        this();
        this.project = project;
        this.taskName = taskName;
        this.type = type;
    }

    public Task(String project, String taskName, String type, double hours, String comment) {
        this(project, taskName, type);
        this.hours = hours;
        this.comment = comment;
    }

    // Геттеры и сеттеры
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

    @Override
    public String toString() {
        return String.format("%s - %s (%s)", project, taskName, type);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Task task = (Task) obj;
        return project.equals(task.project) &&
                taskName.equals(task.taskName) &&
                type.equals(task.type);
    }
}
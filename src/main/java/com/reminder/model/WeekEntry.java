package com.reminder.model;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/** Сводная запись по задаче за неделю: часы по дням. */
public class WeekEntry implements Serializable {
    private static final long serialVersionUID = 1L;
    private String project;
    private String taskName;
    private String type;
    private Map<LocalDate, Integer> dayHours;
    private String comment;

    public WeekEntry() {
        this.dayHours = new HashMap<>();
        this.comment = "";
    }

    public WeekEntry(String project, String taskName, String type) {
        this();
        this.project = project;
        this.taskName = taskName;
        this.type = type;
    }

    public String getProject() { return project; }
    public void setProject(String project) { this.project = project; }

    public String getTaskName() { return taskName; }
    public void setTaskName(String taskName) { this.taskName = taskName; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public Map<LocalDate, Integer> getDayHours() { return dayHours; }
    public void setDayHours(Map<LocalDate, Integer> dayHours) {
        this.dayHours = dayHours != null ? dayHours : new HashMap<>();
    }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }

    public int getTotal() {
        return dayHours.values().stream().mapToInt(Integer::intValue).sum();
    }
}
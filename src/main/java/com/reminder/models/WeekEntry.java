package com.reminder.models;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

public class WeekEntry {
    private String project;
    private String taskName;
    private String type;
    private Map<LocalDate, Integer> dayHours;
    private String comment;

    public WeekEntry(String project, String taskName, String type) {
        this.project = project;
        this.taskName = taskName;
        this.type = type;
        this.dayHours = new HashMap<>();
        this.comment = "";
    }

    public String getProject() { return project; }
    public void setProject(String project) { this.project = project; }

    public String getTaskName() { return taskName; }
    public void setTaskName(String taskName) { this.taskName = taskName; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public Map<LocalDate, Integer> getDayHours() { return dayHours; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }

    public int getTotal() {
        return dayHours.values().stream().mapToInt(Integer::intValue).sum();
    }
}
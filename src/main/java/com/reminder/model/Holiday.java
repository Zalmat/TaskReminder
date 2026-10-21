package com.reminder.model;

import java.time.LocalDate;
import java.util.UUID;

/** Праздник или информационное событие дня. */
public class Holiday {

    private String id;
    private LocalDate date;
    private String name;
    private boolean annual;
    private HolidayType type;
    private HolidaySource source;

    public Holiday() {
        this.id = UUID.randomUUID().toString();
        this.annual = true;
        this.type = HolidayType.OFFICIAL;
        this.source = HolidaySource.MANUAL;
    }

    public Holiday(LocalDate date, String name, boolean annual,
                   HolidayType type, HolidaySource source) {
        this();
        this.date = date;
        this.name = name;
        this.annual = annual;
        this.type = type == null ? HolidayType.OFFICIAL : type;
        this.source = source == null ? HolidaySource.MANUAL : source;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isAnnual() {
        return annual;
    }

    public void setAnnual(boolean annual) {
        this.annual = annual;
    }

    public HolidayType getType() {
        return type == null ? HolidayType.OFFICIAL : type;
    }

    public void setType(HolidayType type) {
        this.type = type == null ? HolidayType.OFFICIAL : type;
    }

    public HolidaySource getSource() {
        return source == null ? HolidaySource.MANUAL : source;
    }

    public void setSource(HolidaySource source) {
        this.source = source == null ? HolidaySource.MANUAL : source;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Holiday holiday)) {
            return false;
        }
        return id != null && id.equals(holiday.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    @Override
    public String toString() {
        return (date == null ? "" : date.toString()) + " " + (name == null ? "" : name);
    }
}

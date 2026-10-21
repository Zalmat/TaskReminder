package com.reminder.storage;

import com.reminder.model.Holiday;

import java.util.List;

/** Хранилище праздников и событий. */
public interface HolidayRepository {

    boolean exists();

    List<Holiday> load();

    void save(List<Holiday> holidays);
}

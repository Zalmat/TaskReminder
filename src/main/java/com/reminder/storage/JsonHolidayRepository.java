package com.reminder.storage;

import com.reminder.model.Holiday;
import com.reminder.model.HolidaySource;
import com.reminder.model.HolidayType;
import com.reminder.model.HolidaysData;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** JSON-реализация хранилища праздников, включая старый формат-массив. */
public final class JsonHolidayRepository implements HolidayRepository {

    private static final String FILE = "holidays.json";

    private final DataStore store;

    public JsonHolidayRepository(DataStore store) {
        this.store = store;
    }

    @Override
    public boolean exists() {
        return store.exists(FILE);
    }

    @Override
    public List<Holiday> load() {
        Path file = store.resolve(FILE);
        if (!Files.isRegularFile(file)) {
            return new ArrayList<>();
        }
        String text;
        try {
            text = Files.readString(file, StandardCharsets.UTF_8).trim();
        } catch (IOException e) {
            throw new StorageException(FILE, "Не удалось прочитать " + FILE, e);
        }

        if (text.startsWith("[")) {
            List<LocalDate> dates = store.loadList(FILE, LocalDate.class);
            List<Holiday> result = new ArrayList<>();
            for (LocalDate date : dates) {
                if (date != null) {
                    result.add(new Holiday(date, "", false, HolidayType.OFFICIAL,
                            HolidaySource.MANUAL));
                }
            }
            return result;
        }

        HolidaysData data = store.loadObject(FILE, HolidaysData.class);
        if (data == null || data.holidays == null) {
            return new ArrayList<>();
        }
        List<Holiday> result = new ArrayList<>();
        data.holidays.stream().filter(h -> h != null).forEach(result::add);
        return result;
    }

    @Override
    public void save(List<Holiday> holidays) {
        HolidaysData data = new HolidaysData();
        data.holidays = new ArrayList<>(holidays);
        store.saveObject(FILE, data);
    }
}

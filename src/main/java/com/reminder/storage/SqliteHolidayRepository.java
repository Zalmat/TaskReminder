package com.reminder.storage;

import com.reminder.model.Holiday;
import com.reminder.model.HolidaySource;
import com.reminder.model.HolidayType;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** SQLite-хранилище праздников. */
public final class SqliteHolidayRepository implements HolidayRepository {

    private static final String INITIALIZED = "holidays_initialized";

    private final SqliteDatabase database;

    public SqliteHolidayRepository(SqliteDatabase database) {
        this.database = database;
    }

    @Override
    public boolean exists() {
        return database.read(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT value FROM storage_flags WHERE flag = ?")) {
                statement.setString(1, INITIALIZED);
                try (ResultSet rows = statement.executeQuery()) {
                    return rows.next() && "1".equals(rows.getString(1));
                }
            }
        });
    }

    @Override
    public List<Holiday> load() {
        return database.read(connection -> {
            List<Holiday> result = new ArrayList<>();
            try (PreparedStatement statement = connection.prepareStatement("""
                    SELECT id, holiday_date, name, annual, type, source
                    FROM holidays ORDER BY rowid
                    """);
                 ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    Holiday holiday = new Holiday();
                    holiday.setId(rows.getString("id"));
                    String date = rows.getString("holiday_date");
                    if (date != null) {
                        holiday.setDate(LocalDate.parse(date));
                    }
                    holiday.setName(rows.getString("name"));
                    holiday.setAnnual(rows.getInt("annual") != 0);
                    holiday.setType(parseType(rows.getString("type")));
                    holiday.setSource(parseSource(rows.getString("source")));
                    result.add(holiday);
                }
            }
            return result;
        });
    }

    @Override
    public void save(List<Holiday> holidays) {
        database.write(connection -> {
            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate("DELETE FROM holidays");
            }
            try (PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO holidays (id, holiday_date, name, annual, type, source)
                    VALUES (?, ?, ?, ?, ?, ?)
                    """)) {
                for (Holiday holiday : holidays) {
                    statement.setString(1, holiday.getId());
                    statement.setString(2, holiday.getDate() == null
                            ? null : holiday.getDate().toString());
                    statement.setString(3, holiday.getName());
                    statement.setInt(4, holiday.isAnnual() ? 1 : 0);
                    statement.setString(5, holiday.getType().name());
                    statement.setString(6, holiday.getSource().name());
                    statement.addBatch();
                }
                statement.executeBatch();
            }
            try (PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO storage_flags (flag, value) VALUES (?, '1')
                    ON CONFLICT(flag) DO UPDATE SET value = '1'
                    """)) {
                statement.setString(1, INITIALIZED);
                statement.executeUpdate();
            }
            return null;
        });
    }

    private static HolidayType parseType(String value) {
        try {
            return value == null ? HolidayType.OFFICIAL : HolidayType.valueOf(value);
        } catch (IllegalArgumentException e) {
            return HolidayType.OFFICIAL;
        }
    }

    private static HolidaySource parseSource(String value) {
        try {
            return value == null ? HolidaySource.MANUAL : HolidaySource.valueOf(value);
        } catch (IllegalArgumentException e) {
            return HolidaySource.MANUAL;
        }
    }
}

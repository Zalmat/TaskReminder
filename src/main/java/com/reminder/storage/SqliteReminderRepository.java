package com.reminder.storage;

import com.reminder.model.Reminder;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/** SQLite-хранилище напоминаний. */
public final class SqliteReminderRepository implements ReminderRepository {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

    private final SqliteDatabase database;

    public SqliteReminderRepository(SqliteDatabase database) {
        this.database = database;
    }

    @Override
    public List<Reminder> load() {
        return database.read(connection -> {
            List<Reminder> result = new ArrayList<>();
            try (PreparedStatement statement = connection.prepareStatement("""
                    SELECT id, time, text, active, repeat_daily, snooze_until
                    FROM reminders ORDER BY rowid
                    """);
                 ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    Reminder reminder = new Reminder();
                    reminder.setId(rows.getString("id"));
                    String time = rows.getString("time");
                    if (time != null) {
                        reminder.setTime(LocalTime.parse(time, TIME));
                    }
                    reminder.setText(rows.getString("text"));
                    reminder.setActive(rows.getInt("active") != 0);
                    reminder.setRepeatDaily(rows.getInt("repeat_daily") != 0);
                    String snooze = rows.getString("snooze_until");
                    if (snooze != null) {
                        reminder.setSnoozeUntil(LocalDateTime.parse(snooze));
                    }
                    result.add(reminder);
                }
            }
            return result;
        });
    }

    @Override
    public void save(List<Reminder> reminders) {
        database.write(connection -> {
            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate("DELETE FROM reminders");
            }
            try (PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO reminders (id, time, text, active, repeat_daily, snooze_until)
                    VALUES (?, ?, ?, ?, ?, ?)
                    """)) {
                for (Reminder reminder : reminders) {
                    statement.setString(1, reminder.getId());
                    statement.setString(2, reminder.getTime() == null
                            ? null : reminder.getTime().format(TIME));
                    statement.setString(3, reminder.getText());
                    statement.setInt(4, reminder.isActive() ? 1 : 0);
                    statement.setInt(5, reminder.isRepeatDaily() ? 1 : 0);
                    statement.setString(6, reminder.getSnoozeUntil() == null
                            ? null : reminder.getSnoozeUntil().toString());
                    statement.addBatch();
                }
                statement.executeBatch();
            }
            return null;
        });
    }
}

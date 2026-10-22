package com.reminder.storage;

import com.reminder.model.WorkEntry;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** SQLite-хранилище записей. */
public final class SqliteWorkEntryRepository implements WorkEntryRepository {

    private final SqliteDatabase database;

    public SqliteWorkEntryRepository(SqliteDatabase database) {
        this.database = database;
    }

    @Override
    public List<WorkEntry> load() {
        return database.read(connection -> {
            List<WorkEntry> result = new ArrayList<>();
            try (PreparedStatement statement = connection.prepareStatement("""
                    SELECT entry_date, project, task_name, type, hours, comment, created_at
                    FROM work_entries ORDER BY id
                    """);
                 ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    WorkEntry entry = new WorkEntry(
                            rows.getString("project"),
                            rows.getString("task_name"),
                            rows.getString("type"),
                            rows.getDouble("hours"));
                    entry.setDate(LocalDate.parse(rows.getString("entry_date")));
                    entry.setComment(rows.getString("comment"));
                    String createdAt = rows.getString("created_at");
                    if (createdAt != null) {
                        entry.setCreatedAt(LocalDateTime.parse(createdAt));
                    }
                    result.add(entry);
                }
            }
            return result;
        });
    }

    @Override
    public void save(List<WorkEntry> entries) {
        database.write(connection -> {
            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate("DELETE FROM work_entries");
            }
            try (PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO work_entries
                        (entry_date, project, task_name, type, hours, comment, created_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?)
                    """)) {
                for (WorkEntry entry : entries) {
                    statement.setString(1, entry.getDate().toString());
                    statement.setString(2, entry.getProject());
                    statement.setString(3, entry.getTaskName());
                    statement.setString(4, entry.getType());
                    statement.setDouble(5, entry.getHours());
                    statement.setString(6, entry.getComment());
                    statement.setString(7, entry.getCreatedAt() == null
                            ? null : entry.getCreatedAt().toString());
                    statement.addBatch();
                }
                statement.executeBatch();
            }
            return null;
        });
    }
}

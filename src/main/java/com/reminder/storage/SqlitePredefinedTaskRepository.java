package com.reminder.storage;

import com.reminder.model.Task;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/** SQLite-хранилище предопределённых задач. */
public final class SqlitePredefinedTaskRepository implements PredefinedTaskRepository {

    private final SqliteDatabase database;

    public SqlitePredefinedTaskRepository(SqliteDatabase database) {
        this.database = database;
    }

    @Override
    public List<Task> load() {
        return database.read(connection -> {
            List<Task> result = new ArrayList<>();
            try (PreparedStatement statement = connection.prepareStatement("""
                    SELECT project, task_name, type, hours, comment
                    FROM predefined_tasks ORDER BY id
                    """);
                 ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    result.add(new Task(rows.getString("project"), rows.getString("task_name"),
                            rows.getString("type"), rows.getDouble("hours"),
                            rows.getString("comment")));
                }
            }
            return result;
        });
    }

    @Override
    public void save(List<Task> tasks) {
        database.write(connection -> {
            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate("DELETE FROM predefined_tasks");
            }
            try (PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO predefined_tasks (project, task_name, type, hours, comment)
                    VALUES (?, ?, ?, ?, ?)
                    """)) {
                for (Task task : tasks) {
                    statement.setString(1, task.getProject());
                    statement.setString(2, task.getTaskName());
                    statement.setString(3, task.getType());
                    statement.setDouble(4, task.getHours());
                    statement.setString(5, task.getComment());
                    statement.addBatch();
                }
                statement.executeBatch();
            }
            return null;
        });
    }
}

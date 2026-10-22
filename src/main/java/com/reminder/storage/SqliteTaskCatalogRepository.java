package com.reminder.storage;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/** SQLite-хранилище каталога подстановки. */
public final class SqliteTaskCatalogRepository implements TaskCatalogRepository {

    private static final String PROJECTS = "PROJECTS";
    private static final String TASKS = "TASKS";
    private static final String TYPES = "TYPES";

    private final SqliteDatabase database;

    public SqliteTaskCatalogRepository(SqliteDatabase database) {
        this.database = database;
    }

    @Override
    public TaskCatalogSnapshot load() {
        return database.read(connection -> {
            List<String> projects = new ArrayList<>();
            List<String> tasks = new ArrayList<>();
            List<String> types = new ArrayList<>();
            try (PreparedStatement statement = connection.prepareStatement("""
                    SELECT kind, value FROM catalog_values
                    ORDER BY kind, position
                    """);
                 ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    switch (rows.getString("kind")) {
                        case PROJECTS -> projects.add(rows.getString("value"));
                        case TASKS -> tasks.add(rows.getString("value"));
                        case TYPES -> types.add(rows.getString("value"));
                        default -> { }
                    }
                }
            }
            return new TaskCatalogSnapshot(projects, tasks, types);
        });
    }

    @Override
    public void save(TaskCatalogSnapshot snapshot) {
        database.write(connection -> {
            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate("DELETE FROM catalog_values");
            }
            try (PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO catalog_values (kind, value, position) VALUES (?, ?, ?)
                    """)) {
                addAll(statement, PROJECTS, snapshot.projects());
                addAll(statement, TASKS, snapshot.taskNames());
                addAll(statement, TYPES, snapshot.types());
                statement.executeBatch();
            }
            return null;
        });
    }

    private static void addAll(PreparedStatement statement, String kind, List<String> values)
            throws java.sql.SQLException {
        for (int i = 0; i < values.size(); i++) {
            statement.setString(1, kind);
            statement.setString(2, values.get(i));
            statement.setInt(3, i);
            statement.addBatch();
        }
    }
}

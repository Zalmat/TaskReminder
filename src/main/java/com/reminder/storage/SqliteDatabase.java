package com.reminder.storage;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/** SQLite-соединение и схема данных версии 2. */
public final class SqliteDatabase implements AutoCloseable {

    public static final String FILE = "taskreminder.db";
    public static final int SCHEMA_VERSION = 2;

    private final Connection connection;

    public SqliteDatabase(Path path) {
        Connection opened = null;
        try {
            opened = DriverManager.getConnection("jdbc:sqlite:" + path.toAbsolutePath());
            this.connection = opened;
            configure();
            ensureSchema();
        } catch (SQLException e) {
            closeQuietly(opened);
            throw new StorageException(FILE, "Не удалось открыть SQLite-базу", e);
        } catch (RuntimeException e) {
            closeQuietly(opened);
            throw e;
        }
    }

    private static void closeQuietly(Connection connection) {
        if (connection == null) {
            return;
        }
        try {
            connection.close();
        } catch (SQLException ignored) {
            // Соединение уже недоступно.
        }
    }

    private void configure() throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys=ON");
            statement.execute("PRAGMA busy_timeout=5000");
            statement.execute("PRAGMA journal_mode=DELETE");
            statement.execute("PRAGMA synchronous=FULL");
        }
    }

    private void ensureSchema() throws SQLException {
        int version = version();
        if (version == 0) {
            createSchema();
            try (Statement statement = connection.createStatement()) {
                statement.execute("PRAGMA user_version=" + SCHEMA_VERSION);
            }
            return;
        }
        if (version != SCHEMA_VERSION) {
            throw new StorageException(FILE,
                    "Неподдерживаемая версия SQLite-схемы: " + version);
        }
    }

    public int version() throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("PRAGMA user_version")) {
            return result.next() ? result.getInt(1) : 0;
        }
    }

    private void createSchema() throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS work_entries (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        entry_date TEXT NOT NULL,
                        project TEXT,
                        task_name TEXT,
                        type TEXT,
                        hours REAL NOT NULL,
                        comment TEXT,
                        created_at TEXT
                    )
                    """);
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS predefined_tasks (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        project TEXT,
                        task_name TEXT,
                        type TEXT,
                        hours REAL,
                        comment TEXT
                    )
                    """);
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS reminders (
                        id TEXT PRIMARY KEY,
                        time TEXT,
                        text TEXT,
                        active INTEGER NOT NULL,
                        repeat_daily INTEGER NOT NULL,
                        snooze_until TEXT
                    )
                    """);
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS holidays (
                        id TEXT PRIMARY KEY,
                        holiday_date TEXT,
                        name TEXT,
                        annual INTEGER NOT NULL,
                        type TEXT,
                        source TEXT
                    )
                    """);
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS catalog_values (
                        kind TEXT NOT NULL,
                        value TEXT NOT NULL,
                        position INTEGER NOT NULL,
                        PRIMARY KEY(kind, value)
                    )
                    """);
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS storage_flags (
                        flag TEXT PRIMARY KEY,
                        value TEXT NOT NULL
                    )
                    """);
        }
    }

    public synchronized <T> T read(SqlWork<T> work) {
        try {
            return work.apply(connection);
        } catch (SQLException e) {
            throw new StorageException(FILE, "Ошибка чтения SQLite", e);
        }
    }

    public synchronized <T> T write(SqlWork<T> work) {
        boolean autoCommit;
        try {
            autoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
        } catch (SQLException e) {
            throw new StorageException(FILE, "Ошибка начала SQLite-транзакции", e);
        }
        try {
            T result = work.apply(connection);
            connection.commit();
            return result;
        } catch (SQLException e) {
            rollbackQuietly();
            throw new StorageException(FILE, "Ошибка записи SQLite", e);
        } catch (RuntimeException e) {
            rollbackQuietly();
            throw e;
        } finally {
            try {
                connection.setAutoCommit(autoCommit);
            } catch (SQLException ignored) {
                // Соединение закрывается при выходе из приложения.
            }
        }
    }

    private void rollbackQuietly() {
        try {
            connection.rollback();
        } catch (SQLException ignored) {
            // Исходная ошибка важнее.
        }
    }

    @Override
    public synchronized void close() {
        try {
            connection.close();
        } catch (SQLException ignored) {
            // Соединение уже могло быть закрыто.
        }
    }

    @FunctionalInterface
    public interface SqlWork<T> {
        T apply(Connection connection) throws SQLException;
    }
}

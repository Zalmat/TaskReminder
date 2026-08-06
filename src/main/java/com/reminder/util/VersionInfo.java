package com.reminder.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/** Версия приложения из сгенерированного ресурса version.properties (см. build.gradle). */
public final class VersionInfo {

    private static final VersionInfo DEFAULT = new VersionInfo("1.0.0", "1.0.0", "unknown", "Zalmat/TaskReminder");

    private final String version;
    private final String numeric;
    private final String commit;
    private final String repository;

    private VersionInfo(String version, String numeric, String commit, String repository) {
        this.version = version;
        this.numeric = numeric;
        this.commit = commit;
        this.repository = repository;
    }

    public static VersionInfo load() {
        Properties props = new Properties();
        try (InputStream in = VersionInfo.class.getResourceAsStream("/version.properties")) {
            if (in != null) {
                props.load(in);
            }
        } catch (IOException ignored) {
            // используется DEFAULT
        }
        return new VersionInfo(
                props.getProperty("version", DEFAULT.version),
                props.getProperty("numeric", DEFAULT.numeric),
                props.getProperty("commit", DEFAULT.commit),
                props.getProperty("repository", DEFAULT.repository));
    }

    public String version() {
        return version;
    }

    public String numeric() {
        return numeric;
    }

    public String commit() {
        return commit;
    }

    public String repository() {
        return repository;
    }
}
package com.reminder.catalog;

import com.reminder.storage.DataStore;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Каталог имён для автоподстановки: проекты, задачи и типы работ.
 * Хранится в {@code task_catalog.json}, пополняется только добавлением (add-only),
 * дубликаты определяются по имени (с учётом регистра).
 */
public class TaskCatalogService {

    private static final String FILE = "task_catalog.json";

    private enum Kind { PROJECTS, TASKS, TYPES }

    private final DataStore store;
    private final Set<String> projects = new LinkedHashSet<>();
    private final Set<String> taskNames = new LinkedHashSet<>();
    private final Set<String> types = new LinkedHashSet<>();

    public TaskCatalogService() {
        this(new DataStore());
    }

    public TaskCatalogService(DataStore store) {
        this.store = store;
        load();
    }

    public synchronized List<String> getProjects() {
        return new ArrayList<>(projects);
    }

    public synchronized List<String> getTaskNames() {
        return new ArrayList<>(taskNames);
    }

    public synchronized List<String> getTypes() {
        return new ArrayList<>(types);
    }

    /** Добавляет имена из результата импорта; возвращает количество новых. */
    public synchronized CatalogAddResult merge(ImportResult result) {
        if (result == null) {
            return CatalogAddResult.empty();
        }
        TaskCatalogData candidate = snapshot();
        int addedProjects = addAll(candidate.projects, result.projects());
        int addedTasks = addAll(candidate.taskNames, result.taskNames());
        int addedTypes = addAll(candidate.types, result.types());
        CatalogAddResult added = new CatalogAddResult(addedProjects, addedTasks, addedTypes);
        if (added.hasNew()) {
            commit(candidate);
        }
        return added;
    }

    /** Засевает типы по умолчанию, если набор типов ещё пуст. */
    public synchronized void ensureDefaultTypes(List<String> defaults) {
        if (!types.isEmpty() || defaults == null) {
            return;
        }
        TaskCatalogData candidate = snapshot();
        if (addAll(candidate.types, defaults) > 0) {
            commit(candidate);
        }
    }

    public synchronized boolean removeProject(String value) {
        return remove(Kind.PROJECTS, value);
    }

    public synchronized boolean removeTaskName(String value) {
        return remove(Kind.TASKS, value);
    }

    public synchronized boolean removeType(String value) {
        return remove(Kind.TYPES, value);
    }

    /** Добавляет тип вручную; пустые и дубликаты игнорируются. */
    public synchronized boolean addType(String value) {
        return add(Kind.TYPES, value);
    }

    /** Добавляет проект вручную; пустые и дубликаты игнорируются. */
    public synchronized boolean addProject(String value) {
        return add(Kind.PROJECTS, value);
    }

    /** Добавляет задачу вручную; пустые и дубликаты игнорируются. */
    public synchronized boolean addTaskName(String value) {
        return add(Kind.TASKS, value);
    }

    public synchronized int clearProjects() {
        return clear(Kind.PROJECTS);
    }

    public synchronized int clearTaskNames() {
        return clear(Kind.TASKS);
    }

    public synchronized int clearTypes() {
        return clear(Kind.TYPES);
    }

    private boolean remove(Kind kind, String value) {
        String name = normalize(value);
        if (name.isEmpty() || !values(kind).contains(name)) {
            return false;
        }
        TaskCatalogData candidate = snapshot();
        candidateValues(candidate, kind).remove(name);
        commit(candidate);
        return true;
    }

    private boolean add(Kind kind, String value) {
        String name = normalize(value);
        if (name.isEmpty() || values(kind).contains(name)) {
            return false;
        }
        TaskCatalogData candidate = snapshot();
        candidateValues(candidate, kind).add(name);
        commit(candidate);
        return true;
    }

    private int clear(Kind kind) {
        Set<String> current = values(kind);
        int count = current.size();
        if (count > 0) {
            TaskCatalogData candidate = snapshot();
            candidateValues(candidate, kind).clear();
            commit(candidate);
        }
        return count;
    }

    private Set<String> values(Kind kind) {
        return switch (kind) {
            case PROJECTS -> projects;
            case TASKS -> taskNames;
            case TYPES -> types;
        };
    }

    private static List<String> candidateValues(TaskCatalogData data, Kind kind) {
        return switch (kind) {
            case PROJECTS -> data.projects;
            case TASKS -> data.taskNames;
            case TYPES -> data.types;
        };
    }

    private TaskCatalogData snapshot() {
        TaskCatalogData data = new TaskCatalogData();
        data.projects = new ArrayList<>(projects);
        data.taskNames = new ArrayList<>(taskNames);
        data.types = new ArrayList<>(types);
        return data;
    }

    private void commit(TaskCatalogData candidate) {
        store.saveObject(FILE, candidate);
        projects.clear();
        projects.addAll(candidate.projects);
        taskNames.clear();
        taskNames.addAll(candidate.taskNames);
        types.clear();
        types.addAll(candidate.types);
    }

    private void load() {
        TaskCatalogData data = store.loadObject(FILE, TaskCatalogData.class);
        if (data == null) {
            return;
        }
        addAll(projects, data.projects);
        addAll(taskNames, data.taskNames);
        addAll(types, data.types);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private static int addAll(Collection<String> target, Collection<String> source) {
        if (source == null) {
            return 0;
        }
        Set<String> seen = new HashSet<>(target);
        int added = 0;
        for (String value : source) {
            String name = normalize(value);
            if (!name.isEmpty() && seen.add(name)) {
                target.add(name);
                added++;
            }
        }
        return added;
    }
}

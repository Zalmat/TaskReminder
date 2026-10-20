package com.reminder.catalog;

import com.reminder.storage.DataStore;

import java.util.ArrayList;
import java.util.Collection;
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

    public List<String> getProjects() {
        return new ArrayList<>(projects);
    }

    public List<String> getTaskNames() {
        return new ArrayList<>(taskNames);
    }

    public List<String> getTypes() {
        return new ArrayList<>(types);
    }

    /** Добавляет имена из результата импорта; возвращает количество новых. */
    public CatalogAddResult merge(ImportResult result) {
        if (result == null) {
            return CatalogAddResult.empty();
        }
        int addedProjects = addAll(projects, result.projects());
        int addedTasks = addAll(taskNames, result.taskNames());
        int addedTypes = addAll(types, result.types());
        CatalogAddResult added = new CatalogAddResult(addedProjects, addedTasks, addedTypes);
        if (added.hasNew()) {
            save();
        }
        return added;
    }

    /** Засевает типы по умолчанию, если набор типов ещё пуст. */
    public void ensureDefaultTypes(List<String> defaults) {
        if (!types.isEmpty() || defaults == null) {
            return;
        }
        if (addAll(types, defaults) > 0) {
            save();
        }
    }

    public boolean removeProject(String value) {
        return remove(projects, value);
    }

    public boolean removeTaskName(String value) {
        return remove(taskNames, value);
    }

    public boolean removeType(String value) {
        return remove(types, value);
    }

    /** Добавляет тип вручную; пустые и дубликаты игнорируются. */
    public boolean addType(String value) {
        String name = value == null ? "" : value.trim();
        if (name.isEmpty() || !types.add(name)) {
            return false;
        }
        save();
        return true;
    }

    public int clearProjects() {
        return clear(projects);
    }

    public int clearTaskNames() {
        return clear(taskNames);
    }

    public int clearTypes() {
        return clear(types);
    }

    private boolean remove(Set<String> target, String value) {
        String name = value == null ? "" : value.trim();
        if (name.isEmpty() || !target.remove(name)) {
            return false;
        }
        save();
        return true;
    }

    private int clear(Set<String> target) {
        int count = target.size();
        if (count > 0) {
            target.clear();
            save();
        }
        return count;
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

    private void save() {
        TaskCatalogData data = new TaskCatalogData();
        data.projects = new ArrayList<>(projects);
        data.taskNames = new ArrayList<>(taskNames);
        data.types = new ArrayList<>(types);
        store.saveObject(FILE, data);
    }

    private static int addAll(Set<String> target, Collection<String> source) {
        if (source == null) {
            return 0;
        }
        int added = 0;
        for (String value : source) {
            String name = value == null ? "" : value.trim();
            if (!name.isEmpty() && target.add(name)) {
                added++;
            }
        }
        return added;
    }
}

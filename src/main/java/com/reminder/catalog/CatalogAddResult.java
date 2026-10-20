package com.reminder.catalog;

/** Сколько новых имён реально добавилось в каталог. */
public record CatalogAddResult(int projects, int taskNames, int types) {

    public static CatalogAddResult empty() {
        return new CatalogAddResult(0, 0, 0);
    }

    public boolean hasNew() {
        return projects + taskNames + types > 0;
    }
}

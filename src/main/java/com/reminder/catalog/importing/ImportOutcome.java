package com.reminder.catalog.importing;

import com.reminder.catalog.CatalogAddResult;

/** Итог импорта: файл не изменился либо добавлены новые имена. */
public record ImportOutcome(boolean unchangedFile, CatalogAddResult added, int totalRows, int skipped) {

    public static ImportOutcome unchanged() {
        return new ImportOutcome(true, CatalogAddResult.empty(), 0, 0);
    }

    public static ImportOutcome imported(CatalogAddResult added, int totalRows, int skipped) {
        return new ImportOutcome(false, added, totalRows, skipped);
    }

    public boolean hasNew() {
        return added != null && added.hasNew();
    }
}

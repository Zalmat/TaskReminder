package com.reminder.catalog.importing;

import com.reminder.catalog.CatalogAddResult;
import com.reminder.catalog.ImportResult;
import com.reminder.catalog.TaskCatalogService;
import com.reminder.importing.ImportStateStore;
import com.reminder.util.FileHashes;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Оркестрация импорта: контрольная сумма (пропуск неизменившегося файла),
 * разбор источником, добавление в каталог и сохранение состояния.
 */
public class CatalogImportService {

    private final CatalogImporter importer;
    private final TaskCatalogService catalog;
    private final ImportStateStore state;

    public CatalogImportService(CatalogImporter importer, TaskCatalogService catalog, ImportStateStore state) {
        this.importer = importer;
        this.catalog = catalog;
        this.state = state;
    }

    /**
     * @param force {@code true} — игнорировать контрольную сумму (ручной импорт).
     */
    public ImportOutcome importFile(Path file, boolean force) throws IOException {
        String hash = FileHashes.sha256(file);
        if (!force && state.isUnchanged(file, hash)) {
            return ImportOutcome.unchanged();
        }
        ImportResult result = importer.importFrom(file);
        CatalogAddResult added = catalog.merge(result);
        state.markImported(file, hash);
        return ImportOutcome.imported(added, result.totalRows(), result.skipped());
    }
}

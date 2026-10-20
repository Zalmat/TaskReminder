package com.reminder.catalog.importing;

import com.reminder.catalog.ImportResult;

import java.io.IOException;
import java.nio.file.Path;

/** Источник имён для каталога (YAML-файл, в будущем URL/JSON и т.п.). */
public interface CatalogImporter {

    ImportResult importFrom(Path file) throws IOException;
}

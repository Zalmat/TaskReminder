package com.reminder.ui.importing;

import com.reminder.catalog.importing.CatalogImportService;
import com.reminder.catalog.importing.ImportOutcome;
import com.reminder.catalog.importing.YamlCatalogImporter;
import com.reminder.i18n.I18n;
import com.reminder.importing.ImportScheduler;
import com.reminder.importing.ImportStateStore;
import com.reminder.service.TaskService;
import com.reminder.settings.AppSettings;
import com.reminder.storage.StorageException;
import com.reminder.suggest.SuggestionService;
import com.reminder.ui.ImportNotifier;
import com.reminder.ui.component.EntryForm;
import com.reminder.ui.util.Dialogs;
import com.reminder.util.FileValidation;
import com.reminder.util.Log;
import com.reminder.util.YamlTemplate;
import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.function.BiConsumer;

/** UI-оркестрация импорта категорий: файл, шаблон, расписание, уведомления. */
public class TaskImportCoordinator {

    private final TaskService taskService;
    private final SuggestionService suggestionService;
    private final EntryForm form;
    private final CatalogImportService catalogImportService;
    private final ImportScheduler scheduler = new ImportScheduler();
    private final ImportNotifier notifier;

    public TaskImportCoordinator(TaskService taskService, SuggestionService suggestionService,
                                 EntryForm form, Label bell,
                                 BiConsumer<String, String> trayNotifier) {
        this.taskService = taskService;
        this.suggestionService = suggestionService;
        this.form = form;
        this.catalogImportService = new CatalogImportService(
                new YamlCatalogImporter(), taskService.getCatalog(), new ImportStateStore());
        this.notifier = new ImportNotifier(bell, trayNotifier);
    }

    /** Запускает расписание и импорт при старте. */
    public void start() {
        applySchedule();
        Platform.runLater(this::runStartupImportIfEnabled);
    }

    /** Перечитывает настройки ежедневного импорта. */
    public void applySchedule() {
        boolean enabled = AppSettings.taskImportScheduleEnabled() && configuredFile() != null;
        scheduler.applySchedule(enabled, AppSettings.taskImportHour(),
                AppSettings.taskImportMinute(), this::scheduledImport);
    }

    public void importFromChooser(Window owner) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(I18n.get("yaml.chooseTitle"));
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter(I18n.get("yaml.filter"), "*.yaml", "*.yml"));
        File file = chooser.showOpenDialog(owner);
        if (file == null) {
            return;
        }
        if (!FileValidation.isReadableYaml(file.toPath())) {
            Dialogs.error(I18n.get("common.error"), I18n.get("yaml.invalidFile"));
            return;
        }
        ImportOutcome outcome = runImport(file.toPath(), true, false);
        if (outcome != null) {
            showResult(outcome);
        }
    }

    /** Импорт из окна настроек (ответ «Да»). */
    public void importFromSettings(Path file) {
        ImportOutcome outcome = runImport(file, true, false);
        if (outcome != null) {
            showResult(outcome);
        }
    }

    public void downloadTemplate(Window owner) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(I18n.get("yaml.saveTitle"));
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter(I18n.get("yaml.filter"), "*.yaml", "*.yml"));
        chooser.setInitialFileName("tasks-template.yaml");
        File file = chooser.showSaveDialog(owner);
        if (file == null) {
            return;
        }
        try {
            Files.writeString(file.toPath(), YamlTemplate.content(), StandardCharsets.UTF_8);
            Dialogs.info(I18n.get("common.success"), I18n.format("yaml.saved", file.getName()));
        } catch (Exception e) {
            Dialogs.error(I18n.get("common.error"), I18n.format("yaml.saveError", e.getMessage()));
        }
    }

    public void shutdown() {
        scheduler.shutdown();
    }

    private void runStartupImportIfEnabled() {
        if (!AppSettings.taskImportOnStartup()) {
            return;
        }
        Path file = configuredFile();
        if (file != null) {
            runImport(file, false, true);
        }
    }

    private void scheduledImport() {
        Platform.runLater(() -> {
            Path file = configuredFile();
            if (file != null) {
                runImport(file, false, true);
            }
        });
    }

    private Path configuredFile() {
        String path = AppSettings.taskImportFile();
        if (path == null || path.isBlank()) {
            return null;
        }
        Path file;
        try {
            file = Path.of(path);
        } catch (RuntimeException e) {
            Log.log("TaskImport", "invalid configured path: " + path, e);
            return null;
        }
        if (!FileValidation.isReadableYaml(file)) {
            Log.log("TaskImport", "configured file is not a readable YAML: " + path);
            return null;
        }
        return file;
    }

    private ImportOutcome runImport(Path file, boolean force, boolean notify) {
        try {
            ImportOutcome outcome = catalogImportService.importFile(file, force);
            if (outcome.unchangedFile()) {
                Log.log("TaskImport", "unchanged: " + file);
                return outcome;
            }
            Log.log("TaskImport", "imported " + file + ": projects+" + outcome.added().projects()
                    + ", tasks+" + outcome.added().taskNames() + ", types+" + outcome.added().types()
                    + " (rows=" + outcome.totalRows() + ", skipped=" + outcome.skipped() + ")");
            if (outcome.hasNew()) {
                suggestionService.reloadCatalog();
                form.refreshSuggestionsOnly();
                form.refreshTypeChoices(taskService);
                if (notify) {
                    notifier.notifyImported(LocalDateTime.now(), outcome,
                            AppSettings.taskImportTrayNotify());
                }
            }
            return outcome;
        } catch (StorageException e) {
            Log.log("TaskImport", "failed: " + e.getMessage(), e);
            if (force) {
                Dialogs.error(I18n.get("common.error"),
                        I18n.get("error.save") + "\n" + Dialogs.reason(e));
            }
            return null;
        } catch (Exception e) {
            Log.log("TaskImport", "failed: " + e.getMessage(), e);
            if (force) {
                Dialogs.error(I18n.get("common.error"),
                        I18n.format("tasks.import.error", Dialogs.reason(e)));
            }
            return null;
        }
    }

    private void showResult(ImportOutcome outcome) {
        if (outcome.hasNew()) {
            Dialogs.info(I18n.get("common.success"), I18n.format("tasks.import.result",
                    outcome.added().projects(), outcome.added().taskNames(), outcome.added().types()));
        } else {
            Dialogs.info(I18n.get("common.success"), I18n.get("tasks.import.nothingNew"));
        }
    }
}

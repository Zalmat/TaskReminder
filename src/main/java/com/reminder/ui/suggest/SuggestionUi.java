package com.reminder.ui.suggest;

import com.reminder.service.TaskService;
import com.reminder.suggest.SuggestionService;
import com.reminder.ui.SuggestionIndexingCoordinator;
import com.reminder.ui.component.EntryForm;
import com.reminder.ui.dialog.SuggestionManagerDialog;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Duration;

/** UI-часть подсказок: индикатор индексации, менеджер и обновление формы. */
public class SuggestionUi {

    private final VBox statusBox;
    private final ProgressBar progressBar;
    private final SuggestionService suggestions;
    private final TaskService taskService;
    private final EntryForm form;

    private SuggestionIndexingCoordinator indexing;
    private Timeline poller;

    public SuggestionUi(VBox statusBox, ProgressBar progressBar,
                        SuggestionService suggestions, TaskService taskService,
                        EntryForm form) {
        this.statusBox = statusBox;
        this.progressBar = progressBar;
        this.suggestions = suggestions;
        this.taskService = taskService;
        this.form = form;
    }

    /** Подключает фоновую индексацию после установки сцены. */
    public void attachStage(Stage stage) {
        if (stage == null || indexing != null) {
            return;
        }
        indexing = new SuggestionIndexingCoordinator(stage, suggestions);
        indexing.start();
        startPolling();
    }

    public void onDataChanged() {
        if (indexing != null) {
            indexing.onDataChanged();
        }
    }

    public void openManager(Window owner) {
        SuggestionManagerDialog.show(owner, taskService, suggestions,
                this::catalogChanged,
                () -> {
                    if (indexing != null) {
                        indexing.refreshNow();
                    }
                });
    }

    public void catalogChanged() {
        suggestions.reloadCatalog();
        form.refreshSuggestionsOnly();
        form.refreshTypeChoices(taskService);
    }

    public void settingsChanged() {
        if (indexing != null) {
            indexing.onSettingsChanged();
        }
        form.refreshSuggestionsOnly();
    }

    public void shutdown() {
        if (indexing != null) {
            indexing.shutdown();
        }
        if (poller != null) {
            poller.stop();
            poller = null;
        }
    }

    private void startPolling() {
        if (poller != null) {
            return;
        }
        poller = new Timeline(new KeyFrame(Duration.millis(200), event -> updateStatus()));
        poller.setCycleCount(Animation.INDEFINITE);
        poller.play();
    }

    private void updateStatus() {
        if (statusBox == null) {
            return;
        }
        if (!suggestions.isIndexing()) {
            statusBox.setVisible(false);
            statusBox.setManaged(false);
            progressBar.setVisible(false);
            progressBar.setManaged(false);
            return;
        }
        long elapsed = System.currentTimeMillis() - suggestions.indexingStartedAtMillis();
        if (elapsed < 400) {
            statusBox.setVisible(false);
            statusBox.setManaged(false);
            return;
        }
        statusBox.setVisible(true);
        statusBox.setManaged(true);
        boolean showProgress = elapsed >= 2000;
        progressBar.setVisible(showProgress);
        progressBar.setManaged(showProgress);
        if (showProgress) {
            progressBar.setProgress(suggestions.progress());
        }
    }
}

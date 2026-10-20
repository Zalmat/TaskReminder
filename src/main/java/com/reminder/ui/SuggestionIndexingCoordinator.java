package com.reminder.ui;

import com.reminder.settings.AppSettings;
import com.reminder.suggest.SuggestionService;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.stage.Stage;
import javafx.util.Duration;

/**
 * Политика фоновой индексации истории: старт, сворачивание, потеря фокуса
 * и настройка «только когда окно неактивно».
 */
public class SuggestionIndexingCoordinator {

    private static final Duration FOCUS_DELAY = Duration.seconds(5);
    private static final Duration DATA_DEBOUNCE = Duration.millis(600);

    private final Stage stage;
    private final SuggestionService service;

    private final PauseTransition focusDelay = new PauseTransition(FOCUS_DELAY);
    private final PauseTransition debounce = new PauseTransition(DATA_DEBOUNCE);

    private boolean dirty;

    public SuggestionIndexingCoordinator(Stage stage, SuggestionService service) {
        this.stage = stage;
        this.service = service;

        focusDelay.setOnFinished(event -> {
            if (!isActive() && dirty) {
                schedule(0);
            }
        });
        debounce.setOnFinished(event -> rebuild());
    }

    /** Подключает слушатели окна и запускает первую индексацию. */
    public void start() {
        stage.focusedProperty().addListener((obs, wasFocused, focused) -> {
            if (Boolean.TRUE.equals(focused)) {
                focusDelay.stop();
            } else {
                focusDelay.playFromStart();
            }
        });
        stage.iconifiedProperty().addListener((obs, wasIconified, iconified) -> {
            if (Boolean.TRUE.equals(iconified)) {
                onBecameInactive();
            }
        });
        stage.showingProperty().addListener((obs, wasShowing, showing) -> {
            if (!Boolean.TRUE.equals(showing)) {
                onBecameInactive();
            }
        });

        // Первая индексация нужна всегда, независимо от настройки.
        Platform.runLater(this::rebuild);
    }

    /** Данные истории изменились. */
    public void onDataChanged() {
        dirty = true;
        if (!AppSettings.indexOnlyWhenInactive() || !isActive()) {
            schedule((long) DATA_DEBOUNCE.toMillis());
        }
    }

    /** Изменились настройки индексации/окно истории. */
    public void onSettingsChanged() {
        dirty = true;
        schedule(0);
    }

    /** Принудительный пересчёт (кнопка «Обновить» в менеджере). */
    public void refreshNow() {
        dirty = false;
        debounce.stop();
        focusDelay.stop();
        service.requestHistoryRebuild();
    }

    public void shutdown() {
        focusDelay.stop();
        debounce.stop();
    }

    private void onBecameInactive() {
        if (dirty && AppSettings.indexOnlyWhenInactive()) {
            schedule(0);
        }
    }

    private void schedule(long delayMillis) {
        debounce.stop();
        debounce.setDuration(Duration.millis(Math.max(0, delayMillis)));
        debounce.playFromStart();
    }

    private void rebuild() {
        dirty = false;
        service.requestHistoryRebuild();
    }

    private boolean isActive() {
        return stage != null && stage.isShowing() && !stage.isIconified() && stage.isFocused();
    }
}

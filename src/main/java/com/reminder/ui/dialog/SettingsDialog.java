package com.reminder.ui.dialog;

import com.reminder.i18n.I18n;
import com.reminder.ui.controller.SettingsDialogController;
import javafx.scene.Scene;
import javafx.stage.Window;

import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;

/** Показ окна настроек. */
public final class SettingsDialog {

    private SettingsDialog() {
    }

    public static void show(Scene mainScene, List<String> workTypes, Consumer<Path> importNow,
                            Consumer<Window> openManager, Runnable onSuggestionsChanged,
                            Runnable onClosed, Runnable onRestart) {
        DialogSupport.show("/fxml/settings-dialog.fxml", I18n.get("dialog.settings.title"),
                SettingsDialogController.class,
                controller -> controller.init(mainScene, workTypes, importNow, openManager,
                        onSuggestionsChanged, onRestart),
                onClosed, "dialog.settings.error");
    }
}

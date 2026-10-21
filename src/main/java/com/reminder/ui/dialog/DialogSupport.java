package com.reminder.ui.dialog;

import com.reminder.i18n.I18n;
import com.reminder.ui.theme.ThemeManager;
import com.reminder.ui.util.AppIcons;
import com.reminder.ui.util.Dialogs;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;

import java.io.IOException;
import java.util.function.Consumer;

/** Загрузка и показ FXML-диалогов с единым оформлением. */
public final class DialogSupport {

    private DialogSupport() {
    }

    public static <T> void show(String fxml, String title, Class<T> controllerType,
                                Consumer<T> initializer, Runnable onClosed, String errorKey) {
        try {
            FXMLLoader loader = new FXMLLoader(DialogSupport.class.getResource(fxml));
            loader.setResources(I18n.bundle());
            DialogPane pane = loader.load();
            T controller = controllerType.cast(loader.getController());
            initializer.accept(controller);
            ThemeManager.applyTo(pane);

            Dialog<Void> dialog = new Dialog<>();
            dialog.setTitle(title);
            dialog.setDialogPane(pane);
            AppIcons.applyTo(dialog);
            dialog.showAndWait();

            if (onClosed != null) {
                onClosed.run();
            }
        } catch (IOException e) {
            Dialogs.error(I18n.get("common.error"), I18n.format(errorKey, Dialogs.reason(e)));
        }
    }
}

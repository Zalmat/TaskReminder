package com.reminder.ui.dialog;

import com.reminder.i18n.I18n;
import com.reminder.service.ReminderService;
import com.reminder.ui.controller.RemindersDialogController;

/** Показ окна напоминаний. */
public final class RemindersDialog {

    private RemindersDialog() {
    }

    public static void show(ReminderService reminderService) {
        DialogSupport.show("/fxml/reminders-dialog.fxml", I18n.get("dialog.reminders.title"),
                RemindersDialogController.class,
                controller -> controller.init(reminderService),
                null, "dialog.reminders.error");
    }
}

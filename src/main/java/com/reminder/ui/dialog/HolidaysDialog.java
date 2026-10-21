package com.reminder.ui.dialog;

import com.reminder.i18n.I18n;
import com.reminder.service.WorkTimeService;
import com.reminder.ui.controller.HolidaysDialogController;
import com.reminder.ui.util.Dialogs;

/** Показ окна управления праздниками. */
public final class HolidaysDialog {

    private HolidaysDialog() {
    }

    public static void show(WorkTimeService workTimeService, Runnable onClosed) {
        DialogSupport.show("/fxml/holidays-dialog.fxml", I18n.get("dialog.holidays.title"),
                HolidaysDialogController.class,
                controller -> controller.init(workTimeService, Dialogs::accept),
                onClosed, "dialog.holidays.error");
    }
}

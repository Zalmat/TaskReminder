package com.reminder.settings;

import com.reminder.i18n.I18n;

import java.util.List;

/** Типы работ по умолчанию (для первичного заполнения каталога). */
public final class WorkTypes {

    private WorkTypes() {
    }

    public static List<String> defaults() {
        return List.of(
                I18n.get("worktype.development"),
                I18n.get("worktype.testing"),
                I18n.get("worktype.design"),
                I18n.get("worktype.analytics"),
                I18n.get("worktype.management"),
                I18n.get("worktype.communication"),
                I18n.get("worktype.other"));
    }
}

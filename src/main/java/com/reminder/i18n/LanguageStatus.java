package com.reminder.i18n;

/** Состояние языкового пакета. */
public enum LanguageStatus {
    /** Все ключи на месте. */
    OK,
    /** Пакет читается, но часть ключей отсутствует. */
    INCOMPLETE,
    /** Пакет повреждён и не может быть применён. */
    DAMAGED
}

package com.reminder.suggest;

import java.time.LocalDate;

/** Значение подстановки с агрегированной статистикой. */
public record Suggestion(String value, String normalized, SuggestionSource source,
                         int uses, LocalDate lastUsed) {

    public Suggestion {
        value = value == null ? "" : value;
        normalized = normalized == null ? "" : normalized;
        source = source == null ? SuggestionSource.HISTORY : source;
        uses = Math.max(0, uses);
    }
}

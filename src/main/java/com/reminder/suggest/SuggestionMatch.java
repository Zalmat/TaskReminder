package com.reminder.suggest;

/** Совпадение для отрисовки: позиция и длина подсветки. */
public record SuggestionMatch(Suggestion suggestion, int matchStart, int matchLength, boolean boosted) {
}

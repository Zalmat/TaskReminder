package com.reminder.suggest;

import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SuggestionRankerTest {

    @Test
    void ranksPrefixBeforeWordStartAndContains() {
        SuggestionIndex index = index(
                List.of(s("Альфа"), s("Альфа-2"), s("Проект Альфа"), s("Большая Альфа")),
                List.of());
        List<SuggestionMatch> result = SuggestionRanker.rank(
                index, SuggestionKind.PROJECT, "аль", null, 10);
        assertEquals(4, result.size());
        assertEquals("Альфа", result.get(0).suggestion().value());
        assertEquals("Альфа-2", result.get(1).suggestion().value());
        assertEquals("Большая Альфа", result.get(2).suggestion().value());
        assertEquals("Проект Альфа", result.get(3).suggestion().value());
        assertEquals(0, result.get(0).matchStart());
        assertEquals(3, result.get(0).matchLength());
    }

    @Test
    void searchIsCaseAndYoInsensitive() {
        SuggestionIndex index = index(List.of(s("Ёлка")), List.of());
        assertEquals(1, SuggestionRanker.rank(index, SuggestionKind.PROJECT, "елка", null, 10).size());
    }

    @Test
    void softBoostKeepsOtherMatches() {
        SuggestionIndex index = index(
                List.of(),
                List.of(s("Встречи с заказчиком"), s("Встреча с подрядчиком")),
                Map.of("встречи с заказчиком", Set.of("альфа")));
        List<SuggestionMatch> result = SuggestionRanker.rank(
                index, SuggestionKind.TASK, "встр", "Альфа", 10);
        assertEquals(2, result.size());
        assertEquals("Встречи с заказчиком", result.get(0).suggestion().value());
        assertTrue(result.get(0).boosted());
        assertTrue(!result.get(1).boosted());
    }

    @Test
    void respectsLimit() {
        SuggestionIndex index = index(List.of(s("A1"), s("A2"), s("A3")), List.of());
        assertEquals(2, SuggestionRanker.rank(index, SuggestionKind.PROJECT, "a", null, 2).size());
    }

    @Test
    void emptyQueryReturnsAllSorted() {
        SuggestionIndex index = index(List.of(s("Б"), s("А")), List.of());
        List<SuggestionMatch> result = SuggestionRanker.rank(
                index, SuggestionKind.PROJECT, "", null, Integer.MAX_VALUE);
        assertEquals(List.of("А", "Б"),
                result.stream().map(match -> match.suggestion().value()).toList());
    }

    private static Suggestion s(String value) {
        return new Suggestion(value, TextNormalizer.key(value), SuggestionSource.HISTORY, 0, null);
    }

    private static SuggestionIndex index(List<Suggestion> projects, List<Suggestion> tasks) {
        return index(projects, tasks, Map.of());
    }

    private static SuggestionIndex index(List<Suggestion> projects, List<Suggestion> tasks,
                                         Map<String, Set<String>> taskProjects) {
        Map<SuggestionKind, List<Suggestion>> values = new EnumMap<>(SuggestionKind.class);
        values.put(SuggestionKind.PROJECT, projects);
        values.put(SuggestionKind.TASK, tasks);
        return new SuggestionIndex(values, taskProjects);
    }
}

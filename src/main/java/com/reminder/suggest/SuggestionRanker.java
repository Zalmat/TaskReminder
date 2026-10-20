package com.reminder.suggest;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Фильтрация и ранжирование подсказок. Без JavaFX и побочных эффектов. */
public final class SuggestionRanker {

    private SuggestionRanker() {
    }

    public static List<SuggestionMatch> rank(SuggestionIndex index, SuggestionKind kind,
                                             String query, String selectedProject, int limit) {
        if (index == null) {
            return List.of();
        }
        List<Suggestion> values = index.values(kind);
        if (values.isEmpty()) {
            return List.of();
        }
        String normalizedQuery = TextNormalizer.matchForm(query == null ? "" : query.trim());
        if (normalizedQuery.isEmpty()) {
            List<Suggestion> all = new ArrayList<>(values);
            all.sort(Comparator.comparing(Suggestion::normalized));
            List<SuggestionMatch> result = new ArrayList<>(Math.min(limit, all.size()));
            for (Suggestion suggestion : all) {
                if (result.size() >= limit) {
                    break;
                }
                result.add(new SuggestionMatch(suggestion, -1, 0, false));
            }
            return result;
        }

        String projectKey = TextNormalizer.key(selectedProject == null ? "" : selectedProject);
        List<Ranked> ranked = new ArrayList<>();
        for (Suggestion suggestion : values) {
            int position = suggestion.normalized().indexOf(normalizedQuery);
            if (position < 0) {
                continue;
            }
            Quality quality = quality(suggestion.normalized(), normalizedQuery, position);
            boolean boosted = kind == SuggestionKind.TASK && !projectKey.isEmpty()
                    && index.projectsForTask(suggestion.normalized()).contains(projectKey);
            ranked.add(new Ranked(new SuggestionMatch(suggestion, position,
                    normalizedQuery.length(), boosted), quality));
        }
        ranked.sort(Comparator
                .comparingInt((Ranked r) -> r.quality.ordinal())
                .thenComparingInt(r -> r.match.boosted() ? 0 : 1)
                .thenComparing(Comparator.comparingInt(
                        (Ranked r) -> r.match.suggestion().uses()).reversed())
                .thenComparing(r -> r.match.suggestion().lastUsed(),
                        Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(r -> r.match.suggestion().normalized()));

        List<SuggestionMatch> result = new ArrayList<>(Math.min(limit, ranked.size()));
        for (Ranked item : ranked) {
            if (result.size() >= limit) {
                break;
            }
            result.add(item.match);
        }
        return result;
    }

    private static Quality quality(String value, String query, int position) {
        if (value.equals(query)) {
            return Quality.EXACT;
        }
        if (position == 0) {
            return Quality.PREFIX;
        }
        char previous = value.charAt(position - 1);
        if (!Character.isLetterOrDigit(previous)) {
            return Quality.WORD_START;
        }
        return Quality.CONTAINS;
    }

    private enum Quality {
        EXACT,
        PREFIX,
        WORD_START,
        CONTAINS
    }

    private record Ranked(SuggestionMatch match, Quality quality) {
    }
}

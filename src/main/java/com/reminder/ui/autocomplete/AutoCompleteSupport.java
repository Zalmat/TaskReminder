package com.reminder.ui.autocomplete;

import com.reminder.suggest.SuggestionKind;
import com.reminder.suggest.SuggestionMatch;
import com.reminder.suggest.SuggestionService;
import javafx.application.Platform;
import javafx.scene.control.ComboBox;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Подключает поиск с подсветкой к обычному редактируемому ComboBox:
 * пустое поле/стрелка — полный список, ввод — фильтрация, свободный текст сохраняется.
 */
public class AutoCompleteSupport {

    private static final int VISIBLE_ROWS = 10;
    private static final int MAX_RESULTS = 50;

    private final ComboBox<String> combo;
    private final SuggestionKind kind;
    private final SuggestionService service;
    private final Supplier<String> projectSupplier;
    private final Map<String, SuggestionMatch> matches = new HashMap<>();

    private boolean updating;
    private boolean showScheduled;
    private boolean showingFromTyping;

    public AutoCompleteSupport(ComboBox<String> combo, SuggestionKind kind,
                               SuggestionService service, Supplier<String> projectSupplier) {
        this.combo = combo;
        this.kind = kind;
        this.service = service;
        this.projectSupplier = projectSupplier == null ? () -> "" : projectSupplier;

        combo.setEditable(true);
        combo.setVisibleRowCount(VISIBLE_ROWS);
        combo.setCellFactory(list -> new SuggestionCell(matches::get, combo::getWidth));
        combo.getEditor().textProperty().addListener((obs, oldText, newText) -> {
            if (!updating) {
                onTextChanged(newText);
            }
        });
        combo.valueProperty().addListener((obs, oldValue, newValue) -> {
            if (!updating && newValue != null) {
                Platform.runLater(() -> {
                    updating = true;
                    try {
                        showFullList();
                    } finally {
                        updating = false;
                    }
                });
            }
        });
        combo.showingProperty().addListener((obs, wasShowing, showing) -> {
            if (Boolean.TRUE.equals(showing)) {
                if (!updating && !showingFromTyping) {
                    String text = editorText();
                    if (text.isEmpty() || text.equals(combo.getValue())) {
                        showFullList();
                    }
                }
                showingFromTyping = false;
            }
        });

        refresh();
    }

    /** Перечитывает текущий объединённый снимок, сохраняя введённый текст. */
    public void refresh() {
        String text = editorText();
        if (text.isEmpty() || text.equals(combo.getValue())) {
            showFullList();
        } else {
            applyQuery(text, false);
        }
    }

    private void onTextChanged(String value) {
        String text = value == null ? "" : value;
        if (text.isEmpty() || text.equals(combo.getValue())) {
            showFullList();
            return;
        }
        applyQuery(text, true);
    }

    private void applyQuery(String query, boolean showPopup) {
        String project = kind == SuggestionKind.TASK ? projectSupplier.get() : null;
        List<SuggestionMatch> result = service.matches(kind, query, project, MAX_RESULTS);
        matches.clear();
        for (SuggestionMatch match : result) {
            matches.put(match.suggestion().value(), match);
        }
        List<String> values = result.stream()
                .map(match -> match.suggestion().value())
                .toList();
        replaceItems(values);
        if (showPopup && !values.isEmpty()) {
            showPopup();
        } else if (values.isEmpty()) {
            combo.hide();
        }
    }

    private void showFullList() {
        matches.clear();
        List<String> values = service.matches(kind, "", null, Integer.MAX_VALUE).stream()
                .map(match -> match.suggestion().value())
                .toList();
        replaceItems(values);
    }

    private void replaceItems(List<String> values) {
        String text = editorText();
        int caret = combo.getEditor().getCaretPosition();
        updating = true;
        try {
            combo.getItems().setAll(values);
            if (!text.equals(editorText())) {
                combo.getEditor().setText(text);
                combo.getEditor().positionCaret(Math.min(caret, text.length()));
            }
        } finally {
            updating = false;
        }
    }

    private void showPopup() {
        if (combo.isShowing() || showScheduled) {
            return;
        }
        showScheduled = true;
        showingFromTyping = true;
        Platform.runLater(() -> {
            showScheduled = false;
            if (!combo.isShowing() && combo.isFocused()) {
                try {
                    combo.show();
                } catch (RuntimeException ignored) {
                    // попап не критичен; текст остаётся в поле
                }
            }
            if (!combo.isShowing()) {
                showingFromTyping = false;
            }
        });
    }

    private String editorText() {
        String text = combo.getEditor().getText();
        return text == null ? "" : text;
    }
}

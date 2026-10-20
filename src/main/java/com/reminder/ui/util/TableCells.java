package com.reminder.ui.util;

import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.Region;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

import java.util.ArrayList;
import java.util.List;

/** Утилиты для ячеек таблиц. */
public final class TableCells {

    private static final double HORIZONTAL_INSETS = 12;
    private static final double TOOLTIP_MAX_WIDTH = 480;
    private static final int MAX_LINES = 3;
    private static final String ELLIPSIS = "…";

    private TableCells() {
    }

    /** Ячейка с визуальным переносом текста (до трёх строк) и подсказкой с полным значением. */
    public static <S> TableCell<S, String> wrapping() {
        return new TableCell<>() {
            private final Label label = new Label();
            private final Tooltip tooltip = Tooltips.create("");
            private final Text measure = new Text();
            private String fullText;
            private double lastWidth = -1;
            private String lastDisplay;

            {
                label.setWrapText(false);
                label.setMinHeight(Region.USE_PREF_SIZE);
                label.setPadding(Insets.EMPTY);
                label.textFillProperty().bind(textFillProperty());
                label.fontProperty().bind(fontProperty());

                tooltip.setWrapText(true);
                tooltip.setMaxWidth(TOOLTIP_MAX_WIDTH);
                setText(null);
                setGraphic(label);

                widthProperty().addListener((obs, oldWidth, newWidth) -> render());
                label.fontProperty().addListener((obs, oldFont, newFont) -> {
                    lastWidth = -1;
                    render();
                });
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                fullText = empty ? null : item;
                lastWidth = -1;
                if (empty || item == null) {
                    label.setText(null);
                    setGraphic(null);
                    setTooltip(null);
                } else {
                    render();
                    setGraphic(label);
                    tooltip.setText(item);
                    setTooltip(tooltip);
                }
            }

            private void render() {
                if (fullText == null) {
                    return;
                }
                double width = getWidth() - HORIZONTAL_INSETS;
                if (width <= 0 || width == lastWidth) {
                    return;
                }
                lastWidth = width;
                Font font = label.getFont() != null ? label.getFont() : Font.getDefault();
                String display = wrap(fullText, width, font);
                if (!display.equals(lastDisplay)) {
                    lastDisplay = display;
                    label.setText(display);
                    label.setMaxWidth(width);
                }
            }

            private String wrap(String text, double width, Font font) {
                measure.setFont(font);
                List<String> lines = new ArrayList<>();
                int index = 0;
                int length = text.length();
                while (index < length && lines.size() < MAX_LINES) {
                    int end = index;
                    int lastSpace = -1;
                    while (end < length) {
                        char ch = text.charAt(end);
                        if (ch == '\n') {
                            break;
                        }
                        measure.setText(text.substring(index, end + 1));
                        if (measure.getLayoutBounds().getWidth() > width) {
                            break;
                        }
                        if (ch == ' ') {
                            lastSpace = end;
                        }
                        end++;
                    }
                    if (end == index) {
                        end = index + 1;
                    }
                    int breakAt = end;
                    if (end < length && lastSpace >= index) {
                        breakAt = lastSpace;
                    }
                    String line = text.substring(index, breakAt).stripTrailing();
                    if (!line.isEmpty()) {
                        lines.add(line);
                    }
                    index = Math.max(breakAt, index + 1);
                    while (index < length && text.charAt(index) == ' ') {
                        index++;
                    }
                }
                if (index < length && !lines.isEmpty()) {
                    int last = lines.size() - 1;
                    lines.set(last, fitEllipsis(lines.get(last), width, font));
                }
                return String.join("\n", lines);
            }

            private String fitEllipsis(String line, double width, Font font) {
                measure.setFont(font);
                String candidate = line + ELLIPSIS;
                while (!line.isEmpty() && measureWidth(candidate) > width) {
                    line = line.substring(0, line.length() - 1).stripTrailing();
                    candidate = line + ELLIPSIS;
                }
                return candidate;
            }

            private double measureWidth(String text) {
                measure.setText(text);
                return measure.getLayoutBounds().getWidth();
            }
        };
    }
}

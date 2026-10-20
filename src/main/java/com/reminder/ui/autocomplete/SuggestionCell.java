package com.reminder.ui.autocomplete;

import com.reminder.suggest.SuggestionMatch;
import com.reminder.ui.util.Tooltips;
import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.scene.control.ListCell;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import java.util.ArrayList;
import java.util.List;
import java.util.function.DoubleSupplier;
import java.util.function.Function;

/** Ячейка подсказки: подсветка совпадения, до двух строк, без изменения значения. */
public class SuggestionCell extends ListCell<String> {

    private static final int MAX_LINES = 2;
    private static final double HORIZONTAL_PADDING = 16;
    private static final String ELLIPSIS = "…";

    private final Function<String, SuggestionMatch> lookup;
    private final DoubleSupplier widthSupplier;
    private final VBox box = new VBox();
    private final Text measure = new Text();
    private final Tooltip tooltip = Tooltips.create("");

    private String currentValue;
    private double lastWidth = -1;

    public SuggestionCell(Function<String, SuggestionMatch> lookup, DoubleSupplier widthSupplier) {
        this.lookup = lookup;
        this.widthSupplier = widthSupplier;
        setText(null);
        setGraphic(box);
        setPadding(new Insets(2, 6, 2, 6));
        box.setFillWidth(true);
        tooltip.setWrapText(true);
        tooltip.setMaxWidth(480);
        widthProperty().addListener((obs, oldWidth, newWidth) -> render());
        fontProperty().addListener((obs, oldFont, newFont) -> {
            lastWidth = -1;
            render();
        });
    }

    @Override
    protected void updateItem(String item, boolean empty) {
        super.updateItem(item, empty);
        currentValue = empty ? null : item;
        lastWidth = -1;
        if (empty || item == null) {
            box.getChildren().clear();
            setTooltip(null);
        } else {
            render();
            tooltip.setText(item);
            setTooltip(tooltip);
        }
    }

    private void render() {
        if (currentValue == null) {
            return;
        }
        double width = getWidth() - HORIZONTAL_PADDING;
        if (width <= 0) {
            width = widthSupplier.getAsDouble() - HORIZONTAL_PADDING;
        }
        if (width <= 0 || width == lastWidth) {
            return;
        }
        lastWidth = width;

        Font font = getFont() != null ? getFont() : Font.getDefault();
        Font bold = Font.font(font.getFamily(), FontWeight.BOLD, font.getSize());
        SuggestionMatch match = lookup == null ? null : lookup.apply(currentValue);
        List<Line> lines = wrap(currentValue, width, font);

        box.getChildren().clear();
        for (int i = 0; i < lines.size(); i++) {
            Line line = lines.get(i);
            TextFlow flow = new TextFlow();
            flow.setMaxWidth(width);
            if (line.truncated()) {
                addSegments(flow, line, match, font, bold);
                Text ellipsis = new Text(ELLIPSIS);
                ellipsis.setFont(font);
                bindFill(ellipsis);
                flow.getChildren().add(ellipsis);
            } else {
                addSegments(flow, line, match, font, bold);
            }
            box.getChildren().add(flow);
        }
    }

    private void addSegments(TextFlow flow, Line line, SuggestionMatch match,
                             Font font, Font bold) {
        String text = currentValue;
        int lineStart = line.start();
        int lineEnd = line.end();
        int matchStart = match == null ? -1 : match.matchStart();
        int matchEnd = match == null ? -1 : matchStart + match.matchLength();

        if (matchStart < 0 || matchEnd <= lineStart || matchStart >= lineEnd) {
            flow.getChildren().add(segment(text.substring(lineStart, lineEnd), font, false));
            return;
        }

        int localMatchStart = Math.max(lineStart, matchStart);
        int localMatchEnd = Math.min(lineEnd, matchEnd);
        if (localMatchStart > lineStart) {
            flow.getChildren().add(segment(text.substring(lineStart, localMatchStart), font, false));
        }
        flow.getChildren().add(segment(text.substring(localMatchStart, localMatchEnd), bold, true));
        if (localMatchEnd < lineEnd) {
            flow.getChildren().add(segment(text.substring(localMatchEnd, lineEnd), font, false));
        }
    }

    private Text segment(String text, Font font, boolean highlighted) {
        Text node = new Text(text);
        node.setFont(font);
        bindFill(node);
        if (highlighted) {
            node.setStyle("-fx-font-weight: bold;");
        }
        return node;
    }

    private void bindFill(Text node) {
        node.fillProperty().bind(Bindings.createObjectBinding(
                () -> getTextFill() != null ? getTextFill() : Color.BLACK,
                textFillProperty()));
    }

    private List<Line> wrap(String text, double width, Font font) {
        measure.setFont(font);
        List<Line> lines = new ArrayList<>();
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
            int lineEnd = breakAt;
            while (lineEnd > index && text.charAt(lineEnd - 1) == ' ') {
                lineEnd--;
            }
            if (lineEnd > index) {
                lines.add(new Line(index, lineEnd, false));
            }
            index = Math.max(breakAt, index + 1);
            while (index < length && text.charAt(index) == ' ') {
                index++;
            }
        }
        boolean truncated = index < length;
        if (truncated && !lines.isEmpty()) {
            Line last = lines.remove(lines.size() - 1);
            lines.add(new Line(last.start(), last.end(), true));
        }
        if (lines.isEmpty()) {
            int end = Math.min(length, Math.max(1, length));
            lines.add(new Line(0, end, false));
        }
        return lines;
    }

    private record Line(int start, int end, boolean truncated) {
    }
}

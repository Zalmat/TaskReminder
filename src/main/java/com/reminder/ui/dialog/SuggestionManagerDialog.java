package com.reminder.ui.dialog;

import com.reminder.catalog.TaskCatalogService;
import com.reminder.i18n.I18n;
import com.reminder.service.TaskService;
import com.reminder.suggest.Suggestion;
import com.reminder.suggest.SuggestionKind;
import com.reminder.suggest.SuggestionService;
import com.reminder.suggest.SuggestionSource;
import com.reminder.suggest.TextNormalizer;
import com.reminder.ui.theme.ThemeManager;
import com.reminder.ui.util.AppIcons;
import com.reminder.ui.util.Dialogs;
import com.reminder.ui.util.TableCells;
import com.reminder.ui.util.WrappingTableRow;
import com.reminder.util.DateUtils;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Window;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;

/** Диалог управления каталогом подстановки: проекты, задачи, типы. */
public final class SuggestionManagerDialog {

    private SuggestionManagerDialog() {
    }

    public static void show(Window owner, TaskService taskService, SuggestionService suggestions,
                            Runnable onCatalogChanged, Runnable onRefreshRequested) {
        TaskCatalogService catalog = taskService.getCatalog();

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle(I18n.get("suggest.manager.title"));
        dialog.initOwner(owner);
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.getDialogPane().getButtonTypes().add(
                new ButtonType(I18n.get("button.close"), ButtonBar.ButtonData.CANCEL_CLOSE));
        dialog.setResizable(true);

        List<Runnable> reloaders = new ArrayList<>();
        Label status = new Label();
        status.getStyleClass().add("hint");

        TabPane tabs = new TabPane();
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabs.getTabs().addAll(
                new Tab(I18n.get("suggest.tab.projects"),
                        valuesTab(SuggestionKind.PROJECT, catalog, suggestions,
                                onCatalogChanged, onRefreshRequested, reloaders, status)),
                new Tab(I18n.get("suggest.tab.tasks"),
                        valuesTab(SuggestionKind.TASK, catalog, suggestions,
                                onCatalogChanged, onRefreshRequested, reloaders, status)),
                new Tab(I18n.get("suggest.tab.types"),
                        typesTab(catalog, suggestions, onCatalogChanged, reloaders, status)));

        BorderPane root = new BorderPane(tabs);
        root.setBottom(status);
        BorderPane.setMargin(status, new Insets(6, 10, 6, 10));
        root.setPrefWidth(780);
        root.setPrefHeight(520);
        dialog.getDialogPane().setContent(root);
        ThemeManager.applyTo(dialog.getDialogPane());
        AppIcons.applyTo(dialog);

        long[] lastBuild = {suggestions.lastBuildFinishedAtMillis()};
        Timeline poller = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
            long finished = suggestions.lastBuildFinishedAtMillis();
            if (finished != lastBuild[0]) {
                lastBuild[0] = finished;
                reloaders.forEach(Runnable::run);
                status.setText(I18n.get("suggest.refreshed"));
            }
        }));
        poller.setCycleCount(Animation.INDEFINITE);
        poller.play();
        dialog.setOnHidden(event -> poller.stop());

        reloaders.forEach(Runnable::run);
        dialog.showAndWait();
    }

    private static VBox valuesTab(SuggestionKind kind, TaskCatalogService catalog,
                                  SuggestionService suggestions, Runnable onCatalogChanged,
                                  Runnable onRefreshRequested, List<Runnable> reloaders,
                                  Label status) {
        ObservableList<Suggestion> data = FXCollections.observableArrayList();
        FilteredList<Suggestion> filtered = new FilteredList<>(data, item -> true);
        String[] pinned = {null};
        TableView<Suggestion> table = new TableView<>(filtered);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setFixedCellSize(-1);
        table.setRowFactory(tv -> new WrappingTableRow<>());
        table.widthProperty().addListener((obs, oldWidth, newWidth) -> table.refresh());

        TableColumn<Suggestion, String> valueColumn =
                new TableColumn<>(I18n.get("suggest.column.value"));
        valueColumn.setCellValueFactory(cd -> new ReadOnlyStringWrapper(cd.getValue().value()));
        valueColumn.setCellFactory(column -> TableCells.wrapping());
        valueColumn.setPrefWidth(280);
        TableColumn<Suggestion, String> sourceColumn =
                new TableColumn<>(I18n.get("suggest.column.source"));
        sourceColumn.setCellValueFactory(cd -> new ReadOnlyStringWrapper(sourceLabel(cd.getValue().source())));
        sourceColumn.setPrefWidth(130);
        TableColumn<Suggestion, Integer> usesColumn =
                new TableColumn<>(I18n.get("suggest.column.uses"));
        usesColumn.setCellValueFactory(cd -> new ReadOnlyObjectWrapper<>(cd.getValue().uses()));
        usesColumn.setPrefWidth(90);
        TableColumn<Suggestion, String> lastColumn =
                new TableColumn<>(I18n.get("suggest.column.lastUsed"));
        lastColumn.setCellValueFactory(cd -> new ReadOnlyStringWrapper(
                cd.getValue().lastUsed() == null ? "" : DateUtils.formatDate(cd.getValue().lastUsed())));
        lastColumn.setPrefWidth(130);
        table.getColumns().addAll(List.of(valueColumn, sourceColumn, usesColumn, lastColumn));

        TextField search = new TextField();
        search.setPromptText(I18n.get("suggest.search"));
        search.textProperty().addListener((obs, oldText, newText) -> {
            String query = TextNormalizer.matchForm(newText == null ? "" : newText.trim());
            filtered.setPredicate(item -> query.isEmpty() || item.normalized().contains(query));
        });

        Button delete = new Button(I18n.get("suggest.delete"));
        delete.setOnAction(event -> {
            List<Suggestion> selected = new ArrayList<>(table.getSelectionModel().getSelectedItems());
            if (selected.isEmpty()) {
                return;
            }
            if (!Dialogs.confirm(I18n.get("suggest.manager.title"),
                    I18n.get("suggest.delete.confirm"), I18n.get("suggest.delete.warning"))) {
                return;
            }
            for (Suggestion item : selected) {
                if (kind == SuggestionKind.PROJECT) {
                    catalog.removeProject(item.value());
                } else {
                    catalog.removeTaskName(item.value());
                }
            }
            onCatalogChanged.run();
            reloadAll(reloaders);
            if (selected.stream().anyMatch(item -> item.source() != SuggestionSource.CATALOG)) {
                status.setText(I18n.get("suggest.historyRemains"));
            }
        });

        Button clear = new Button(I18n.get("suggest.clear"));
        clear.setOnAction(event -> {
            if (!Dialogs.confirm(I18n.get("suggest.manager.title"),
                    I18n.get("suggest.clear.confirm"), I18n.get("suggest.clear.warning"))) {
                return;
            }
            if (kind == SuggestionKind.PROJECT) {
                catalog.clearProjects();
            } else {
                catalog.clearTaskNames();
            }
            onCatalogChanged.run();
            reloadAll(reloaders);
        });

        Button refresh = new Button(I18n.get("suggest.refresh"));
        refresh.setOnAction(event -> {
            onRefreshRequested.run();
            status.setText(I18n.get("suggest.refreshStarted"));
        });

        TextField newValue = new TextField();
        newValue.setPromptText(kind == SuggestionKind.PROJECT
                ? I18n.get("suggest.newProject") : I18n.get("suggest.newTask"));
        Button add = new Button(kind == SuggestionKind.PROJECT
                ? I18n.get("suggest.addProject") : I18n.get("suggest.addTask"));
        add.setOnAction(event -> {
            String value = newValue.getText();
            if (value == null || value.isBlank()) {
                Dialogs.error(I18n.get("common.error"), I18n.get("suggest.valueEmpty"));
                return;
            }
            String clean = value.trim();
            boolean added = kind == SuggestionKind.PROJECT
                    ? catalog.addProject(clean) : catalog.addTaskName(clean);
            if (!added) {
                Dialogs.info(I18n.get("suggest.manager.title"), I18n.get("suggest.valueExists"));
                return;
            }
            newValue.clear();
            pinned[0] = TextNormalizer.key(clean);
            onCatalogChanged.run();
            search.clear();
            reloadAll(reloaders);
            if (!data.isEmpty()) {
                table.getSelectionModel().select(0);
                table.scrollTo(0);
            }
        });
        newValue.setOnAction(event -> add.fire());

        Label warning = new Label(I18n.get("suggest.warning"));
        warning.setWrapText(true);
        warning.getStyleClass().add("hint");

        HBox searchRow = new HBox(8, search);
        HBox.setHgrow(search, Priority.ALWAYS);
        HBox addRow = new HBox(8, newValue, add, delete, clear, refresh);
        HBox.setHgrow(newValue, Priority.ALWAYS);

        VBox box = new VBox(8, searchRow, table, addRow, warning);
        box.setPadding(new Insets(10));
        VBox.setVgrow(table, Priority.ALWAYS);

        reloaders.add(() -> {
            List<Suggestion> list = new ArrayList<>(suggestions.stats(kind));
            if (pinned[0] != null) {
                for (int i = 0; i < list.size(); i++) {
                    if (list.get(i).normalized().equals(pinned[0])) {
                        Suggestion item = list.remove(i);
                        list.add(0, item);
                        break;
                    }
                }
            }
            data.setAll(list);
        });
        return box;
    }

    private static VBox typesTab(TaskCatalogService catalog, SuggestionService suggestions,
                                 Runnable onCatalogChanged, List<Runnable> reloaders, Label status) {
        ObservableList<Suggestion> data = FXCollections.observableArrayList();
        FilteredList<Suggestion> filtered = new FilteredList<>(data, item -> true);
        TableView<Suggestion> table = new TableView<>(filtered);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setFixedCellSize(-1);
        table.setRowFactory(tv -> new WrappingTableRow<>());
        table.widthProperty().addListener((obs, oldWidth, newWidth) -> table.refresh());

        TableColumn<Suggestion, String> valueColumn =
                new TableColumn<>(I18n.get("suggest.column.value"));
        valueColumn.setCellValueFactory(cd -> new ReadOnlyStringWrapper(cd.getValue().value()));
        valueColumn.setCellFactory(column -> TableCells.wrapping());
        valueColumn.setPrefWidth(320);
        TableColumn<Suggestion, Integer> usesColumn =
                new TableColumn<>(I18n.get("suggest.column.uses"));
        usesColumn.setCellValueFactory(cd -> new ReadOnlyObjectWrapper<>(cd.getValue().uses()));
        usesColumn.setPrefWidth(100);
        TableColumn<Suggestion, String> lastColumn =
                new TableColumn<>(I18n.get("suggest.column.lastUsed"));
        lastColumn.setCellValueFactory(cd -> new ReadOnlyStringWrapper(
                cd.getValue().lastUsed() == null ? "" : DateUtils.formatDate(cd.getValue().lastUsed())));
        lastColumn.setPrefWidth(140);
        table.getColumns().addAll(List.of(valueColumn, usesColumn, lastColumn));

        TextField search = new TextField();
        search.setPromptText(I18n.get("suggest.search"));
        search.textProperty().addListener((obs, oldText, newText) -> {
            String query = TextNormalizer.matchForm(newText == null ? "" : newText.trim());
            filtered.setPredicate(item -> query.isEmpty() || item.normalized().contains(query));
        });

        TextField newType = new TextField();
        newType.setPromptText(I18n.get("suggest.addType.prompt"));
        Button add = new Button(I18n.get("suggest.addType"));
        String[] pinned = {null};
        add.setOnAction(event -> {
            String value = newType.getText();
            if (value == null || value.isBlank()) {
                Dialogs.error(I18n.get("common.error"), I18n.get("suggest.typeEmpty"));
                return;
            }
            String clean = value.trim();
            if (!catalog.addType(clean)) {
                Dialogs.info(I18n.get("suggest.manager.title"), I18n.get("suggest.typeExists"));
                return;
            }
            newType.clear();
            pinned[0] = TextNormalizer.key(clean);
            onCatalogChanged.run();
            search.clear();
            reloadAll(reloaders);
            if (!data.isEmpty()) {
                table.getSelectionModel().select(0);
                table.scrollTo(0);
            }
        });
        newType.setOnAction(event -> add.fire());

        Button delete = new Button(I18n.get("suggest.delete"));
        delete.setOnAction(event -> {
            List<Suggestion> selected = new ArrayList<>(table.getSelectionModel().getSelectedItems());
            if (selected.isEmpty()) {
                return;
            }
            if (!Dialogs.confirm(I18n.get("suggest.manager.title"),
                    I18n.get("suggest.deleteType.confirm"), I18n.get("suggest.deleteType.warning"))) {
                return;
            }
            for (Suggestion item : selected) {
                catalog.removeType(item.value());
            }
            onCatalogChanged.run();
            reloadAll(reloaders);
        });

        Button clear = new Button(I18n.get("suggest.clear"));
        clear.setOnAction(event -> {
            if (!Dialogs.confirm(I18n.get("suggest.manager.title"),
                    I18n.get("suggest.clearTypes.confirm"), I18n.get("suggest.clearTypes.warning"))) {
                return;
            }
            catalog.clearTypes();
            onCatalogChanged.run();
            reloadAll(reloaders);
        });

        HBox addRow = new HBox(8, newType, add, delete, clear);
        HBox.setHgrow(newType, Priority.ALWAYS);
        HBox searchRow = new HBox(8, search);
        HBox.setHgrow(search, Priority.ALWAYS);

        VBox box = new VBox(8, searchRow, table, addRow);
        box.setPadding(new Insets(10));
        VBox.setVgrow(table, Priority.ALWAYS);

        reloaders.add(() -> {
            List<Suggestion> list = new ArrayList<>(suggestions.typeStats());
            if (pinned[0] != null) {
                for (int i = 0; i < list.size(); i++) {
                    if (list.get(i).normalized().equals(pinned[0])) {
                        Suggestion item = list.remove(i);
                        list.add(0, item);
                        break;
                    }
                }
            }
            data.setAll(list);
        });
        return box;
    }

    private static void reloadAll(List<Runnable> reloaders) {
        reloaders.forEach(Runnable::run);
    }

    private static String sourceLabel(SuggestionSource source) {
        return switch (source) {
            case CATALOG -> I18n.get("suggest.source.catalog");
            case HISTORY -> I18n.get("suggest.source.history");
            default -> I18n.get("suggest.source.both");
        };
    }
}

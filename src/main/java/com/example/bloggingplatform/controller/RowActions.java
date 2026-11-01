package com.example.bloggingplatform.controller;

import com.example.bloggingplatform.util.Icons;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

/** The icon buttons (view, edit, publish, delete, …) in the Actions column of a table. */
final class RowActions {

    private RowActions() {
    }

    /**
     * One button in the Actions column.
     *
     * @param label      tooltip text, computed per row (e.g. "Publish" or "Move back to drafts")
     * @param styleClass colour: {@code btn-update} (orange), {@code btn-delete} (red) or {@code null} (black)
     * @param shownFor   rows the button appears in, e.g. only the user's own comments
     */
    record Action<S>(Function<S, String> label, String icon, String styleClass,
                     Consumer<S> handler, Predicate<S> shownFor) {

        /** The same button, shown only in rows matching {@code condition}. */
        Action<S> onlyWhen(Predicate<S> condition) {
            return new Action<>(label, icon, styleClass, handler, condition);
        }
    }

    static <S> Action<S> view(String label, Consumer<S> handler) {
        return new Action<>(row -> label, Icons.VIEW, null, handler, row -> true);
    }

    static <S> Action<S> edit(Consumer<S> handler) {
        return new Action<>(row -> "Edit", Icons.EDIT, "btn-update", handler, row -> true);
    }

    static <S> Action<S> delete(Consumer<S> handler) {
        return new Action<>(row -> "Delete", Icons.DELETE, "btn-delete", handler, row -> true);
    }

    /**
     * Puts the given buttons in the Actions column. Double-clicking a row runs {@code onDoubleClick}
     * (usually Edit or View); pass null for nothing.
     */
    @SafeVarargs
    static <S> void install(TableView<S> table, TableColumn<S, S> column, Consumer<S> onDoubleClick,
                            Action<S>... actions) {
        column.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue()));
        column.setCellFactory(c -> new ButtonsCell<>(List.of(actions)));
        column.setSortable(false);
        column.setReorderable(false);
        table.setRowFactory(t -> {
            TableRow<S> row = new TableRow<>();
            row.setOnMouseClicked(e -> {
                if (onDoubleClick != null && e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2
                        && !row.isEmpty()) {
                    onDoubleClick.accept(row.getItem());
                }
            });
            return row;
        });
    }

    /** The buttons of one row: small icon buttons with a tooltip naming the action. */
    private static final class ButtonsCell<S> extends TableCell<S, S> {

        private final List<Action<S>> actions;
        private final List<Button> buttons = new ArrayList<>();
        private final HBox box = new HBox(6);

        ButtonsCell(List<Action<S>> actions) {
            this.actions = actions;
            for (Action<S> action : actions) {
                Button button = new Button();
                button.setGraphic(Icons.of(action.icon(), 15));
                button.getStyleClass().add("btn-icon");
                if (action.styleClass() != null) {
                    button.getStyleClass().add(action.styleClass());
                }
                Tooltip tooltip = new Tooltip();
                tooltip.setShowDelay(Duration.millis(250));
                button.setTooltip(tooltip);
                button.setOnAction(e -> action.handler().accept(getItem()));
                buttons.add(button);
            }
            box.getChildren().setAll(buttons);
            box.setAlignment(Pos.CENTER_LEFT);
        }

        @Override
        protected void updateItem(S item, boolean empty) {
            super.updateItem(item, empty);
            setText(null);
            if (empty || item == null) {
                setGraphic(null);
                return;
            }
            for (int i = 0; i < actions.size(); i++) {
                Action<S> action = actions.get(i);
                Button button = buttons.get(i);
                boolean shown = action.shownFor().test(item);
                button.setVisible(shown);
                button.setManaged(shown);
                String label = action.label().apply(item);
                button.getTooltip().setText(label);
                button.setAccessibleText(label);
            }
            setGraphic(box);
        }
    }
}

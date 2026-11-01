package com.example.bloggingplatform.controller;

import com.example.bloggingplatform.service.AppContext;
import com.example.bloggingplatform.dao.DaoException;
import com.example.bloggingplatform.model.User;
import com.example.bloggingplatform.validations.ValidationException;
import com.example.bloggingplatform.util.Icons;
import com.example.bloggingplatform.util.PopupWindow;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.stage.Window;

import java.lang.System.Logger.Level;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Shared behaviour of every screen: the search query from the top bar, pop-up forms, running
 * service calls with user feedback, and small table helpers. Subclasses never catch exceptions
 * themselves. The buttons in table rows are built by {@link RowActions}.
 */
public abstract class BaseController {

    private static final System.Logger LOG = System.getLogger(BaseController.class.getName());
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    protected final AppContext app;
    protected final FormErrors errors = new FormErrors();
    private final ReadOnlyStringWrapper query = new ReadOnlyStringWrapper(this, "query", "");
    protected Navigator navigator;
    protected Scope scope = Scope.ALL;

    protected BaseController(AppContext app) {
        this.app = app;
    }

    /** Reloads everything the screen shows; called whenever the screen is opened. */
    public abstract void refresh();

    /** Hint shown in the search bar while this screen is open. */
    public abstract String searchPrompt();

    /** Opens the pop-up for a new record (the screen's "New" button and Ctrl+N). */
    public abstract void openNewForm();

    /** Shows only the rows matching {@link #query()}. */
    protected abstract void applyQuery();

    /** Any node of the screen, used to find the window that owns pop-ups and toasts. */
    protected abstract Node root();

    /** Applies a search typed in the top bar; blank shows everything again. */
    public final void search(String text) {
        query.set(text == null ? "" : text.strip());
        applyQuery();
    }

    /** The search currently applied to this screen, "" for none. */
    public final String query() {
        return query.get();
    }

    public final ReadOnlyStringProperty queryProperty() {
        return query.getReadOnlyProperty();
    }

    /**
     * Shows {@code form} in a pop-up with Cancel and a Create (green) or Update (orange) button.
     * The pop-up stays open while {@code submit} reports failure, so the user can fix the fields
     * that are marked; it closes when {@code submit} returns {@code true}.
     */
    protected void openForm(String title, Node form, boolean creating, Node firstField, BooleanSupplier submit) {
        openForm(title, form, creating ? "Create" : "Update", creating, firstField, submit);
    }

    /** Like {@link #openForm(String, Node, boolean, Node, BooleanSupplier)} with a custom label, e.g. "Post comment". */
    protected void openForm(String title, Node form, String confirmText, boolean creating, Node firstField,
                            BooleanSupplier submit) {
        PopupWindow popup = new PopupWindow(window(), title);
        popup.setContent(form);
        popup.addButton("Cancel", null, popup::close);
        Button confirm = popup.addButton(confirmText, creating ? "btn-create" : "btn-update", () -> {
            if (submit.getAsBoolean()) {
                popup.close();
            }
        });
        confirm.setGraphic(Icons.of(creating ? Icons.ADD : Icons.CHECK, 14));
        confirm.setDefaultButton(true);   // Enter in a text field saves
        popup.setOnClosed(() -> popup.setContent(null));   // release the form so it can be shown again
        popup.showAndWait(firstField);
    }

    /**
     * Runs a create or update from a pop-up form. On success shows {@code successMessage} as a
     * toast and returns the saved record; on failure marks the offending fields, shows the reason
     * at the top of the form and returns empty.
     */
    protected <T> Optional<T> save(String successMessage, Supplier<T> action) {
        return save(errors, successMessage, action);
    }

    /** Like {@link #save(String, Supplier)}, reporting problems in the given form. */
    protected <T> Optional<T> save(FormErrors target, String successMessage, Supplier<T> action) {
        target.clear();
        try {
            T saved = action.get();
            Dialogs.toast(window(), successMessage);
            return Optional.of(saved);
        } catch (ValidationException e) {
            target.show(e.getMessage(), e.fieldErrors());
        } catch (DaoException e) {
            LOG.log(Level.WARNING, "Database rejected a change", e);
            target.show(e.getMessage(), Map.of());
        }
        return Optional.empty();
    }

    /** The signed-in user (screens only exist while someone is signed in). */
    protected User me() {
        return app.session().require();
    }

    /** Runs a delete: a toast on success, an error pop-up on failure. Returns whether it succeeded. */
    protected boolean perform(String successMessage, Runnable action) {
        try {
            action.run();
            Dialogs.toast(window(), successMessage);
            return true;
        } catch (ValidationException e) {
            Dialogs.error(window(), "Nothing was changed", e.getMessage());
        } catch (DaoException e) {
            LOG.log(Level.WARNING, "Database rejected a change", e);
            Dialogs.error(window(), "The database rejected the change", e.getMessage());
        }
        return false;
    }

    /** Runs a read, showing an error pop-up instead of crashing if the database fails. */
    protected void load(Runnable loader) {
        try {
            loader.run();
        } catch (DaoException e) {
            LOG.log(Level.WARNING, "Could not load data", e);
            Dialogs.error(window(), "Could not load data", e.getMessage());
        }
    }

    protected boolean confirmDelete(String what, String consequence) {
        return Dialogs.confirmDelete(window(), "Delete " + what + "?", consequence + "\nThis cannot be undone.");
    }

    public void setNavigator(Navigator navigator) {
        this.navigator = navigator;
    }

    /** Chooses whose records the screen shows; called once, right after the screen is loaded. */
    public final void setScope(Scope scope) {
        this.scope = scope;
        scopeChanged();
    }

    /** Adjusts titles and buttons to {@link #scope}. */
    protected void scopeChanged() {
    }

    /** Puts a "+" icon on a screen's green "New ..." button. */
    protected static void newButton(Button button) {
        button.setGraphic(Icons.of(Icons.ADD, 15));
    }

    /** Shows {@code value(row)} in a column. */
    protected static <S, T> void column(TableColumn<S, T> column, Function<S, T> value) {
        column.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(value.apply(cell.getValue())));
    }

    protected static String format(LocalDateTime time) {
        return time == null ? "" : DATE_TIME.format(time);
    }

    /** True if any value contains {@code query}, ignoring case; an empty query matches everything. */
    static boolean matches(String query, Object... values) {
        if (query == null || query.isBlank()) {
            return true;
        }
        String needle = query.strip().toLowerCase(Locale.ROOT);
        for (Object value : values) {
            if (value != null && value.toString().toLowerCase(Locale.ROOT).contains(needle)) {
                return true;
            }
        }
        return false;
    }

    /** "12 comments", or "3 of 12 comments match "java"" while a search is applied. */
    protected String countText(int shown, int total, String singular, String plural) {
        String noun = total == 1 ? singular : plural;
        return query().isEmpty() ? total + " " + noun
                : "%d of %d %s match \"%s\"".formatted(shown, total, noun, query());
    }

    protected static <T> T findFirst(List<T> items, Predicate<T> match) {
        return items.stream().filter(match).findFirst().orElse(null);
    }

    protected Window window() {
        Scene scene = root().getScene();
        return scene == null ? null : scene.getWindow();
    }

}

package com.example.bloggingplatform.controller;

import javafx.scene.Node;
import javafx.scene.control.ComboBoxBase;
import javafx.scene.control.Label;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.TextInputControl;
import javafx.scene.layout.Region;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Shows validation messages, in red, in a form. Each field key (the same keys the services use
 * in {@code ValidationException}) is bound to its input control and an error label; the summary
 * label is a banner at the top of the form for errors that are not about one field.
 * <p>
 * Fields with a {@link #live live rule} are also checked while the user works: when they leave a
 * field they typed in, and on every change while the field shows an error, so the message goes
 * away as soon as the value is fixed. The service checks everything again on save.
 */
final class FormErrors {

    static final String INVALID_STYLE = "field-invalid";

    private record Binding(Node control, Label message) {
    }

    private final Map<String, Binding> bindings = new HashMap<>();
    private final Set<String> edited = new HashSet<>();
    private Label summary;

    void bind(String field, Node control, Label message) {
        hide(message);
        message.setMinHeight(Region.USE_PREF_SIZE);   // wrap long messages, never cut them with "..."
        bindings.put(field, new Binding(control, message));
        if (control instanceof ComboBoxBase<?> choice) {
            // picking a value answers "... is required"; the service checks the choice again on save
            choice.valueProperty().addListener((obs, old, value) -> {
                if (value != null && isShown(field)) {
                    unmark(field);
                }
            });
        }
    }

    void bindSummary(Label banner) {
        hide(banner);
        banner.setMinHeight(Region.USE_PREF_SIZE);
        summary = banner;
    }

    /**
     * Checks {@code input} with {@code rule} as the user types (see the class comment). The rule
     * returns the message to show, or {@code null} if the value is fine; use the service's
     * validators in {@code validations/} so the form and the service never disagree.
     *
     * @param alsoOn other fields the rule reads, e.g. the password for "repeat password": a change
     *               there re-checks this field while it shows an error
     */
    void live(String field, TextInputControl input, Supplier<String> rule, TextInputControl... alsoOn) {
        input.textProperty().addListener((obs, old, text) -> {
            if (input.isFocused()) {
                edited.add(field);   // typed by the user, not filled in by the form
            }
            if (isShown(field)) {
                check(field, rule);
            }
        });
        input.focusedProperty().addListener((obs, was, focused) -> {
            if (!focused && edited.contains(field)) {
                check(field, rule);
            }
        });
        for (TextInputControl other : alsoOn) {
            other.textProperty().addListener((obs, old, text) -> {
                if (isShown(field)) {
                    check(field, rule);
                }
            });
        }
    }

    /** Stops the user typing more than {@code max} characters, e.g. into a single-line field. */
    static void limit(TextInputControl input, int max) {
        input.setTextFormatter(new TextFormatter<String>(change ->
                change.getControlNewText().length() <= max ? change : null));
    }

    /** Removes every highlight and message. */
    void clear() {
        for (String field : bindings.keySet()) {
            unmark(field);
        }
        edited.clear();
        if (summary != null) {
            hide(summary);
        }
    }

    /**
     * Highlights each listed field and shows its message. The banner shows {@code message} unless
     * it would only repeat the one message already shown under a field.
     */
    void show(String message, Map<String, String> fieldErrors) {
        fieldErrors.forEach(this::mark);
        boolean onlyOneShownField = fieldErrors.size() == 1 && bindings.containsKey(fieldErrors.keySet().iterator().next());
        if (summary != null && message != null && !onlyOneShownField) {
            reveal(summary, message);
        }
    }

    /** Shows one field's message without touching the others, e.g. for a tag that cannot be added. */
    void showField(String field, String message) {
        mark(field, message);
    }

    /** Removes one field's message. */
    void clearField(String field) {
        if (bindings.containsKey(field)) {
            unmark(field);
        }
    }

    /** Runs a live rule and shows or removes the field's message; keeps the banner's count right. */
    private void check(String field, Supplier<String> rule) {
        String message = rule.get();
        if (message == null) {
            unmark(field);
        } else {
            mark(field, message);
        }
        if (summary != null && summary.isVisible()) {
            long shown = bindings.keySet().stream().filter(this::isShown).count();
            if (shown <= 1) {
                hide(summary);   // a single problem is already explained under its field
            } else {
                reveal(summary, "Please correct the " + shown + " highlighted fields.");
            }
        }
    }

    private void mark(String field, String text) {
        Binding binding = bindings.get(field);
        if (binding == null) {
            return;
        }
        if (!binding.control().getStyleClass().contains(INVALID_STYLE)) {
            binding.control().getStyleClass().add(INVALID_STYLE);
        }
        reveal(binding.message(), text);
    }

    private void unmark(String field) {
        Binding binding = bindings.get(field);
        binding.control().getStyleClass().remove(INVALID_STYLE);
        hide(binding.message());
    }

    private boolean isShown(String field) {
        Binding binding = bindings.get(field);
        return binding != null && binding.message().isVisible();
    }

    private static void reveal(Label label, String text) {
        label.setText("✖ " + text);
        label.setVisible(true);
        label.setManaged(true);
    }

    private static void hide(Label message) {
        message.setText("");
        message.setVisible(false);
        message.setManaged(false);   // takes no space while hidden
    }
}

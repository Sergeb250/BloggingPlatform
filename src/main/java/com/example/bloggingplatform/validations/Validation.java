package com.example.bloggingplatform.validations;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Collects field errors so that a form reports every problem at once instead of one per click.
 * Call the check methods, then {@link #throwIfInvalid()}. The rules themselves are in
 * validator classes ({@link UserValidator}, {@link PostValidator}, ...).
 */
public class Validation {

    private final Map<String, String> errors = new LinkedHashMap<>();

    /**
     * Checks a typed value with a rule from one of the validators.
     *
     * @return the value without leading/trailing whitespace ("" if it was {@code null})
     */
    public String text(String field, String value, Function<String, String> rule) {
        rule(field, rule.apply(value));
        return TextRules.strip(value);
    }

    /**
     * Checks an optional typed value with a rule from one of the validators.
     *
     * @return the stripped value, or {@code null} if it was blank
     */
    public String optionalText(String field, String value, Function<String, String> rule) {
        String stripped = text(field, value, rule);
        return stripped.isEmpty() ? null : stripped;
    }

    /** Records the message of a rule from one of the validators; {@code null} means the value passed. */
    public void rule(String field, String message) {
        if (message != null) {
            reject(field, message);
        }
    }

    /** Checks that a choice (combo box value) was made. */
    public <T> T required(String field, String label, T value) {
        if (value == null) {
            reject(field, label + " is required.");
        }
        return value;
    }

    public void check(boolean condition, String field, String message) {
        if (!condition) {
            reject(field, message);
        }
    }

    /** Records an error; the first error per field wins. */
    public void reject(String field, String message) {
        errors.putIfAbsent(field, message);
    }

    public boolean hasError(String field) {
        return errors.containsKey(field);
    }

    public void throwIfInvalid() {
        if (errors.isEmpty()) {
            return;
        }
        String message = errors.size() == 1
                ? errors.values().iterator().next()
                : "Please correct the " + errors.size() + " highlighted fields.";
        throw new ValidationException(message, errors);
    }
}

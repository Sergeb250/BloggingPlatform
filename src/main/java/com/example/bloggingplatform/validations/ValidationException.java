package com.example.bloggingplatform.validations;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Thrown by services when input breaks a rule. Carries one message per form field so the UI
 * can show each error next to the field it belongs to.
 * <p>
 * Field keys used by the services: {@code login, password} (sign in), {@code username, email,
 * password, confirm, role} (users), {@code author, title, content, status, tags} (posts),
 * {@code post, user, content} (comments), {@code post, user, rating, text} (reviews), {@code name} (tags).
 */
public class ValidationException extends RuntimeException {

    private final Map<String, String> fieldErrors;

    public ValidationException(String message, Map<String, String> fieldErrors) {
        super(message);
        this.fieldErrors = Collections.unmodifiableMap(new LinkedHashMap<>(fieldErrors));
    }

    /** An error that does not belong to a single field, e.g. "This post no longer exists." */
    public static ValidationException of(String message) {
        return new ValidationException(message, Map.of());
    }

    /** Field name to message, in the order the fields were checked. */
    public Map<String, String> fieldErrors() {
        return fieldErrors;
    }
}

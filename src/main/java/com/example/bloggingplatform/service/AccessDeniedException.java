package com.example.bloggingplatform.service;

import com.example.bloggingplatform.validations.ValidationException;

import java.util.Map;

/** Thrown when the signed-in user's role does not allow an action, e.g. a reader deleting a post. */
public class AccessDeniedException extends ValidationException {

    public AccessDeniedException(String message) {
        super(message, Map.of());
    }
}

package com.example.bloggingplatform.validations;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

import static com.example.bloggingplatform.validations.TextRules.hasLetterOrDigit;
import static com.example.bloggingplatform.validations.TextRules.length;
import static com.example.bloggingplatform.validations.TextRules.strip;

/** The rules for a post's title and content. Each method returns the message to show, or null when the value is fine. */
public final class PostValidator {

    public static final int TITLE_MIN = 3;
    public static final int TITLE_MAX = 200;             // posts.title VARCHAR(200)
    public static final int CONTENT_MIN = 20;
    public static final int CONTENT_MAX_BYTES = 65_535;  // posts.content TEXT
    public static final int SEARCH_MAX = 100;            // longest text the search bar accepts

    private PostValidator() {
    }

    public static String title(String value) {
        String title = strip(value);
        if (title.isEmpty()) {
            return "Title is required.";
        }
        int length = length(title);
        if (length < TITLE_MIN) {
            return "Title must be at least " + TITLE_MIN + " characters.";
        }
        if (length > TITLE_MAX) {
            return "Title must be at most " + TITLE_MAX + " characters (now " + length + ").";
        }
        return hasLetterOrDigit(title) ? null : "Title must contain letters or digits, not only symbols.";
    }

    public static String content(String value) {
        String text = strip(value);
        if (text.isEmpty()) {
            return "Content is required.";
        }
        if (length(text) < CONTENT_MIN) {
            return "Content must be at least " + CONTENT_MIN + " characters (now " + length(text) + ").";
        }
        int bytes = text.getBytes(StandardCharsets.UTF_8).length;
        if (bytes > CONTENT_MAX_BYTES) {
            return String.format(Locale.ROOT, "Content is too long: the limit is 65,535 bytes (now %,d).", bytes);
        }
        return hasLetterOrDigit(text) ? null : "Content must contain words, not only symbols.";
    }
}

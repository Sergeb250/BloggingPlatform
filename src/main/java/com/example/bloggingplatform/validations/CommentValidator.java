package com.example.bloggingplatform.validations;

import static com.example.bloggingplatform.validations.TextRules.hasLetterOrDigit;
import static com.example.bloggingplatform.validations.TextRules.length;
import static com.example.bloggingplatform.validations.TextRules.strip;

/** The rule for a comment's text. Returns the message to show, or null when the text is fine. */
public final class CommentValidator {

    public static final int COMMENT_MIN = 2;
    public static final int COMMENT_MAX = 2000;

    private CommentValidator() {
    }

    public static String comment(String value) {
        String text = strip(value);
        if (text.isEmpty()) {
            return "Comment is required.";
        }
        int length = length(text);
        if (length < COMMENT_MIN) {
            return "Comment must be at least " + COMMENT_MIN + " characters.";
        }
        if (length > COMMENT_MAX) {
            return "Comment must be at most " + COMMENT_MAX + " characters (now " + length + ").";
        }
        return hasLetterOrDigit(text) ? null : "Comment must contain words, not only symbols.";
    }
}

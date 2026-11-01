package com.example.bloggingplatform.validations;

import static com.example.bloggingplatform.validations.TextRules.hasLetterOrDigit;
import static com.example.bloggingplatform.validations.TextRules.length;
import static com.example.bloggingplatform.validations.TextRules.strip;

/** The rules for a review: the rating and the optional text. Each method returns the message to show, or null. */
public final class ReviewValidator {

    public static final int REVIEW_MIN = 3;
    public static final int REVIEW_MAX = 5000;

    private ReviewValidator() {
    }

    public static String rating(Integer value) {
        if (value == null) {
            return "Rating is required: choose 1 to 5 stars.";
        }
        return value >= 1 && value <= 5 ? null : "Rating must be between 1 and 5.";
    }

    /** The text of a review is optional; when given it must be real text. */
    public static String reviewText(String value) {
        String text = strip(value);
        if (text.isEmpty()) {
            return null;
        }
        int length = length(text);
        if (length < REVIEW_MIN) {
            return "Review text must be at least " + REVIEW_MIN + " characters, or leave it empty.";
        }
        if (length > REVIEW_MAX) {
            return "Review text must be at most " + REVIEW_MAX + " characters (now " + length + ").";
        }
        return hasLetterOrDigit(text) ? null : "Review text must contain words, not only symbols.";
    }
}

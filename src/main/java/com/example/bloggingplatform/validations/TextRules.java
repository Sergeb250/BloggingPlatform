package com.example.bloggingplatform.validations;

import java.util.List;

/** Small text checks shared by the validators. */
public final class TextRules {

    private TextRules() {
    }

    /** The value without leading and trailing spaces; "" for null. */
    public static String strip(String value) {
        return value == null ? "" : value.strip();
    }

    /** Length in characters as MySQL counts them (an emoji is one character, not two). */
    public static int length(String value) {
        return value.codePointCount(0, value.length());
    }

    public static boolean hasWhitespace(String value) {
        return value.codePoints().anyMatch(Character::isWhitespace);
    }

    public static boolean hasLetterOrDigit(String value) {
        return value.codePoints().anyMatch(Character::isLetterOrDigit);
    }

    /** "a", "a and b", "a, b and c". */
    public static String joined(List<String> parts) {
        if (parts.size() == 1) {
            return parts.getFirst();
        }
        return String.join(", ", parts.subList(0, parts.size() - 1)) + " and " + parts.getLast();
    }
}

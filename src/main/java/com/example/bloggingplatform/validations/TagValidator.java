package com.example.bloggingplatform.validations;

import java.util.Locale;
import java.util.regex.Pattern;

import static com.example.bloggingplatform.validations.TextRules.length;
import static com.example.bloggingplatform.validations.TextRules.strip;

/** The rules for tag names. Tags are stored as lower-case slugs such as {@code spring-boot}. */
public final class TagValidator {

    public static final int NAME_MIN = 2;
    public static final int NAME_MAX = 50;            // tags.name VARCHAR(50)
    public static final int MAX_TAGS_PER_POST = 8;

    /** Lower-case letters and digits in groups joined by single hyphens. */
    private static final Pattern SLUG = Pattern.compile("[a-z0-9]+(?:-[a-z0-9]+)*");

    private TagValidator() {
    }

    /** "  Spring Boot " becomes "spring-boot": trimmed, lower-cased, spaces turned into hyphens. */
    public static String slug(String name) {
        return strip(name).toLowerCase(Locale.ROOT).replaceAll("\\s+", "-");
    }

    /** Checks a tag name as stored, i.e. after {@link #slug}. Returns the message to show, or null. */
    public static String name(String slug) {
        if (slug == null || slug.isEmpty()) {
            return "Tag name is required.";
        }
        int length = length(slug);
        if (length < NAME_MIN || length > NAME_MAX) {
            return "Tag name must be " + NAME_MIN + "-" + NAME_MAX + " characters long.";
        }
        return SLUG.matcher(slug).matches() ? null
                : "Tag names use only letters, digits and single hyphens, e.g. 'spring-boot'.";
    }

    /** One tag of a post, naming it in the message, e.g. {@code "c#": Tag names use only ...}. */
    public static String postTag(String slug) {
        String message = name(slug);
        return message == null ? null : "\"" + slug + "\": " + message;
    }
}

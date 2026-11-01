package com.example.bloggingplatform.validations;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static com.example.bloggingplatform.validations.TextRules.hasWhitespace;
import static com.example.bloggingplatform.validations.TextRules.joined;
import static com.example.bloggingplatform.validations.TextRules.length;
import static com.example.bloggingplatform.validations.TextRules.strip;

/**
 * The rules for account fields: username, email, password and the sign-in form. Each method
 * returns the message to show under the field, or null when the value is fine.
 */
public final class UserValidator {

    public static final int USERNAME_MIN = 3;
    public static final int USERNAME_MAX = 50;      // users.username VARCHAR(50)
    public static final int EMAIL_MAX = 254;        // the longest address mail servers accept
    public static final int EMAIL_NAME_MAX = 64;    // the part before '@'
    public static final int PASSWORD_MIN = 6;
    public static final int PASSWORD_MAX = 64;

    /** Starts with a letter; letters and digits, with single '.', '_' or '-' between them. */
    private static final Pattern USERNAME = Pattern.compile("[A-Za-z][A-Za-z0-9]*(?:[._-][A-Za-z0-9]+)*");

    /**
     * {@code name@domain.ending}. The name uses letters, digits and {@code . _ % + -}, with no dot at
     * either end and no two dots in a row. The domain is one or more labels of letters and digits
     * (hyphens only inside a label, at most 63 characters each), then an ending of 2-24 letters.
     */
    private static final Pattern EMAIL = Pattern.compile(
            "[A-Za-z0-9_%+-]+(?:\\.[A-Za-z0-9_%+-]+)*"
                    + "@(?:[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?\\.)+[A-Za-z]{2,24}");

    private UserValidator() {
    }

    public static String username(String value) {
        String name = strip(value);
        if (name.isEmpty()) {
            return "Username is required.";
        }
        if (hasWhitespace(name)) {
            return "Username cannot contain spaces.";
        }
        int length = length(name);
        if (length < USERNAME_MIN || length > USERNAME_MAX) {
            return "Username must be " + USERNAME_MIN + "-" + USERNAME_MAX + " characters long (now " + length + ").";
        }
        if (!Character.isLetter(name.charAt(0))) {
            return "Username must start with a letter.";
        }
        if (!USERNAME.matcher(name).matches()) {
            return "Username may only use letters, digits and single '.', '_' or '-' between them.";
        }
        return null;
    }

    public static String email(String value) {
        String mail = strip(value);
        if (mail.isEmpty()) {
            return "Email is required.";
        }
        if (hasWhitespace(mail)) {
            return "Email cannot contain spaces.";
        }
        if (mail.length() > EMAIL_MAX) {
            return "Email must be at most " + EMAIL_MAX + " characters.";
        }
        int at = mail.indexOf('@');
        if (at < 0 || at != mail.lastIndexOf('@')) {
            return "Email must contain exactly one '@', e.g. name@example.com.";
        }
        if (at > EMAIL_NAME_MAX) {
            return "The part before '@' can be at most " + EMAIL_NAME_MAX + " characters.";
        }
        if (!EMAIL.matcher(mail).matches()) {
            return "Enter a valid email address, e.g. name@example.com.";
        }
        return null;
    }

    /**
     * A new password: {@value #PASSWORD_MIN}-{@value #PASSWORD_MAX} characters with at least one
     * letter, one number and one symbol, and no spaces. The message names everything still missing.
     */
    public static String password(String value) {
        if (value == null || value.isEmpty()) {
            return "Password is required.";
        }
        if (hasWhitespace(value)) {
            return "Password cannot contain spaces.";
        }
        if (length(value) > PASSWORD_MAX) {
            return "Password must be at most " + PASSWORD_MAX + " characters.";
        }
        List<String> missing = new ArrayList<>();
        if (value.codePoints().noneMatch(Character::isLetter)) {
            missing.add("a letter");
        }
        if (value.codePoints().noneMatch(Character::isDigit)) {
            missing.add("a number");
        }
        if (value.codePoints().allMatch(Character::isLetterOrDigit)) {
            missing.add("a symbol such as ! @ # $ -");
        }
        boolean tooShort = length(value) < PASSWORD_MIN;
        String contain = missing.isEmpty() ? "" : "contain " + joined(missing);
        if (tooShort) {
            return "Password must be at least " + PASSWORD_MIN + " characters long"
                    + (contain.isEmpty() ? "." : " and " + contain + ".");
        }
        return contain.isEmpty() ? null : "Password must also " + contain + ".";
    }

    /** The password field of the edit-user form: blank keeps the current password, anything else must follow {@link #password}. */
    public static String newPassword(String value) {
        return value == null || value.isEmpty() ? null : password(value);
    }

    /** The "repeat password" field of the registration form. */
    public static String confirmPassword(String password, String confirm) {
        if (confirm == null || confirm.isEmpty()) {
            return "Repeat the password.";
        }
        return confirm.equals(password) ? null : "The two passwords do not match.";
    }

    /** The sign-in field: a username or an email address. */
    public static String login(String value) {
        String login = strip(value);
        if (login.isEmpty()) {
            return "Enter your username or email.";
        }
        if (login.indexOf('@') >= 0) {
            return email(login) == null ? null : "Enter a valid email address, e.g. name@example.com.";
        }
        return username(login) == null ? null : "Enter a valid username or email address.";
    }

    /** The password typed to sign in. Only presence is checked; the rules for new passwords may have changed since. */
    public static String loginPassword(String value) {
        return value == null || value.isEmpty() ? "Enter your password." : null;
    }
}

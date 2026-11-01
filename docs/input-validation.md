# Strict input validation

Every value a user types is checked against a strict rule. Any problem is shown **in red**:

- a message under the field;
- a red outline around the field;
- a red banner at the top of the form when there are several problems.

The message appears as soon as the user leaves a wrong field, and it disappears as soon as the
value is fixed. The service checks everything again when the form is saved.

This builds on the Phase 2 validation (requirement 2.1-b in [`phases.md`](../phases.md)). The
schema is unchanged.

---

## 1. What was done

| Before | Now |
|--------|-----|
| Rules spread over five services, each with its own patterns | One validator per subject in `validations/`: `UserValidator`, `PostValidator`, `CommentValidator`, `ReviewValidator`, `TagValidator` |
| Email: anything with an `@` and a dot (`[^@\s]+@[^@\s]+\.[^@\s]+`) | Strict email regex (§2) and a specific message for each kind of mistake |
| Password: at least 8 characters, nothing else | 6–64 characters, at least one letter, one number and one symbol, no spaces |
| Username: 3–50 of `A-Z a-z 0-9 . _ -` | Must start with a letter; `.`, `_` and `-` only singly, between letters or digits |
| Title, content and comment: only "not blank" and a maximum length | Minimum lengths too, and they must contain letters or digits, not only symbols |
| Errors only after clicking Save | Also live: checked when you leave a field, and on each change while a message is shown |
| Messages in black | Messages, outlines and banners in red (`#d93025`, the same red as Delete) |
| Single-line fields accepted any length | Typing stops at the field's maximum, including the search bar (100 characters) |

---

## 2. The rules

The limits are constants in the validators (e.g. `UserValidator.PASSWORD_MIN`). Column sizes are from [`docs/sql/01_schema.sql`](sql/01_schema.sql).

| Field | Rule | Example messages |
|-------|------|------------------|
| **Username** (register, Users screen) | 3–50 characters (`VARCHAR(50)`); starts with a letter; letters and digits, with single `.` `_` `-` between them; no spaces. Regex: `[A-Za-z][A-Za-z0-9]*(?:[._-][A-Za-z0-9]+)*` | "Username must start with a letter." · "Username cannot contain spaces." |
| **Email** | At most 254 characters, at most 64 before the `@`, exactly one `@`, no spaces. The part before the `@` uses letters, digits and `. _ % + -`, with no dot at either end and no two dots in a row. The domain is labels of letters and digits (hyphens only inside a label), then an ending of 2–24 letters. | "Email must contain exactly one '@', e.g. name@example.com." · "Enter a valid email address, e.g. name@example.com." |
| **Password** (register, new user, changed password) | 6–64 characters, at least one letter, one number and one symbol (anything that is not a letter or digit), no spaces | "Password must also contain a number and a symbol such as ! @ # $ -." · "Password must be at least 6 characters long and contain a number." |
| **Repeat password** | Must equal the password | "The two passwords do not match." |
| **Sign-in name** | A valid username or (if it contains `@`) a valid email | "Enter a valid username or email address." |
| **Sign-in password** | Required. Today's password rules are not applied here, so accounts made under the old rule can still sign in. | "Enter your password." |
| **Post title** | 3–200 characters (`VARCHAR(200)`), must contain a letter or digit | "Title must be at least 3 characters." |
| **Post content** | At least 20 characters, at most 65,535 UTF-8 bytes (`TEXT`), must contain a letter or digit | "Content must be at least 20 characters (now 9)." |
| **Tags** (post form, Tags screen) | Saved as a slug ("Spring Boot" becomes `spring-boot`); 2–50 characters; lower-case letters and digits in groups joined by single hyphens; at most 8 per post | "\"c#\": Tag names use only letters, digits and single hyphens, e.g. 'spring-boot'." |
| **Comment** | 2–2000 characters, must contain a letter or digit | "Comment must contain words, not only symbols." |
| **Review text** | Optional; if given, 3–5000 characters with a letter or digit | "Review text must be at least 3 characters, or leave it empty." |
| **Rating** | Required, 1–5 (also a CHECK constraint) | "Rating is required: choose 1 to 5 stars." |
| **Choices** (author, status, post, user, role) | Required | "Status is required." (cleared as soon as something is picked) |
| **Search bar** | At most 100 characters (typing stops) | — |

Lengths are counted in Unicode code points, the way MySQL counts `VARCHAR` characters. Text is
trimmed before it is checked and saved.

**Existing data was checked.** All 9 accounts and every post, comment, tag and review in the
database already meet the new rules, so nothing existing becomes invalid.

---

## 3. How it works

```
         typing / leaving a field                            Save
                    │                                          │
                    ▼                                          ▼
FormErrors.live(field, input, rule) ── same rule ──►  XxxService.create/update
                    │                                          │ Validation.text(field, value, XxxValidator::rule)
                    ▼                                          │ ... every field ...
          red message under the field                          ▼
          (gone once fixed)                       ValidationException {field → message}
                                                               │
                                                               ▼
                                         FormErrors.show → red messages + red banner
```

- **One source of truth.** The validator methods take the raw value and return the message to
  show, or `null` if the value is fine. The services and the forms call the same methods, so
  they can never disagree.
- **The service decides.** Live checks are only feedback. Every service still checks every
  field before saving, collects all problems, and reports them together. A value that slips past
  the form is still rejected.
- **Database constraints are the last line.** UNIQUE and CHECK constraints are still enforced
  by MySQL. For example, "username already taken" comes from `DaoException`.

### Code walkthrough

**`validations/UserValidator.java`.** For example, the password rule names everything that is still
missing:

```java
List<String> missing = new ArrayList<>();
if (value.codePoints().noneMatch(Character::isLetter)) missing.add("a letter");
if (value.codePoints().noneMatch(Character::isDigit))  missing.add("a number");
if (value.codePoints().allMatch(Character::isLetterOrDigit)) missing.add("a symbol such as ! @ # $ -");
boolean tooShort = length(value) < PASSWORD_MIN;
// "Password must be at least 6 characters long and contain a number."
// "Password must also contain a number and a symbol such as ! @ # $ -."
```

**`validations/Validation.java`** applies a rule to a field and returns the trimmed value:

```java
String name = v.text("username", username, UserValidator::username);
String mail = v.text("email", email, UserValidator::email);
v.rule("password", UserValidator.password(password));
v.rule("confirm", UserValidator.confirmPassword(password, confirmPassword));
v.throwIfInvalid();   // all problems at once
```

**`controller/FormErrors.java`** has three new pieces:

- `live(field, input, rule, alsoOn...)`:
  - When the user leaves a field they typed in, it runs the rule.
  - While the field shows a message, it re-runs the rule on every change. The message goes
    away as soon as the value is right.
  - `alsoOn` re-checks "repeat password" when the password changes.
  - It keeps the banner's count correct, and hides the banner when one problem or none is left.
- `limit(input, max)`: a `TextFormatter` that stops typing at the field's maximum.
- Choice boxes: picking a value clears their "... is required" message.

Every form wires its fields in the same way, for example the post form:

```java
errors.live("title", titleField, () -> PostValidator.title(titleField.getText()));
errors.live("content", contentArea, () -> PostValidator.content(contentArea.getText()));
errors.live("tags", newTagField, this::tagProblem);
FormErrors.limit(titleField, PostValidator.TITLE_MAX);
```

**Forms covered:**
- sign in and register (`LoginController`);
- new and edit user (`UserController`);
- new and edit post, including the tag input (`PostController`);
- comment form (`CommentController`), and the comment and rating pop-ups in the reading view
  (`ArticleController`);
- review form (`ReviewController`);
- tag form (`TagController`);
- the search bar (`ShellController`, length limit only).

**Red styling (`app.css`):**
- Palette: `-danger: #d93025` and `-danger-soft: #fce8e6` are added to `.root`.
- `.field-error` messages use red text.
- The `.form-message` banner is pale red with a red bar on the left.
- `.field-invalid` outlines are red on text fields, text areas, combo boxes, the tag input, the
  star picker and the account-type choice.
- The heading of the error pop-up (`Dialogs.error`) is red.

Error labels never shrink below their full height, so long messages wrap instead of being cut
off with "…". The two-column rows of the registration and post forms split their width equally.

---

## 4. Testing evidence

- **Unit tests:** `src/test/java/.../validations/`: one test class per validator, 53 tests. They cover:
  - 5 valid and 16 invalid emails (dots at the ends, double dots, bad domains, numeric endings,
    accented letters, …), plus a specific message for each kind of mistake;
  - passwords: each missing kind of character, too short, too long, spaces, exactly 64
    characters;
  - usernames, sign-in values, title, content, comment, review text, rating and tag slugs.
- **Existing service tests:** updated where they used values that are now too weak (e.g. the
  password `my-password` has no digit, and post bodies such as `"Body"` are shorter than 20
  characters).
- **Full run:** [`testing/phase-2/junit-results.txt`](testing/phase-2/junit-results.txt):
  **142 tests, 0 failures**.
- **In the running app:** a scripted run typed wrong values into the forms. The screenshots are
  in [`testing/input-validation/screenshots/`](testing/input-validation/screenshots/):

| Screenshot | Shows |
|------------|-------|
| `01-register-live.png` | Wrong username, email, password and repeat password: red messages appear as the user moves from field to field |
| `02-register-submitted.png` | After "Create account": the service reports the same 4 problems, with a red banner on top |
| `03-register-fixing.png` | Password and username fixed: their messages are gone at once and the banner counts 2 |
| `04-post-live.png` | Post form: a title that is too short, the tag `c#` and content under 20 characters, all marked in red while typing |
| `05-post-submitted.png` | The same form after "Create": red banner "Please correct the 3 highlighted fields." |

---

## 5. Limitations

- Email is checked for **form**, not for existence. No confirmation mail is sent; that would
  need a mail server.
- The email rule accepts the common address forms only. Rare but valid forms, such as quoted
  local parts (`"john doe"@example.com`) and addresses with accented letters, are rejected on
  purpose: strict is the requirement.
- The password rule is about character variety, not strength: `abc12!` passes. A strength
  meter or a list of common passwords would be a next step.
- Changing a post's status re-checks the whole post. A post saved before these rules with
  under 20 characters of content would have to be edited first. The current data has none.

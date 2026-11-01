# UI Redesign & Role-Based Access

| | |
|---|---|
| **Extends** | Phase 2 — Data Access & CRUD ([phase-2-data-access-crud.md](phase-2-data-access-crud.md)) |
| **Asked for** | A white-first theme (first "liquid black", then white dominating with black only on buttons and icons, after a reference screenshot), sidebar, search bar at the top, action buttons in the tables, pop-up forms, own window controls, a bordered window that does not open full screen, a notebook-and-pen splash screen with a dots loader; sign in and registration with role-based access; a real blog experience for readers, a writing workspace for authors and a management dashboard for admins |
| **Constraint** | No new tables: everything uses the Phase 1 schema (`users.role`, `users.password_hash`, `posts.status`, …) |
| **Evidence** | [`testing/phase-2/junit-results.txt`](testing/phase-2/junit-results.txt) (142 tests, 0 failures) · [`testing/phase-2/screenshots/`](testing/phase-2/screenshots/) |

---

## 1. What changed for the user

| Before | After |
|--------|-------|
| One tab per table, everyone sees everything | Sign-in page; a workspace whose sidebar depends on the role (Reader / Author / Admin) |
| Form on the right of every table | Edit / Delete (and View, Publish) buttons in every row; forms open as pop-up windows |
| Search box inside the Posts screen | One search bar at the top that searches whichever screen is open |
| Operating-system window frame, opened very large | Own light title bar with minimize / maximize / close, rounded bordered window with a shadow, opens at 86 % of the screen and centred, resizable from every edge |
| Success pop-up after every save | A short "toast" message that fades away by itself |
| App appeared after a pause | Splash screen: a pen writing on a notebook page, the app name, and a three-dot loader while MySQL is checked |

### What each role can do

| | Reader | Author | Admin |
|---|:---:|:---:|:---:|
| Discover: read published posts, search, filter by tag / author | ✔ | ✔ | ✔ |
| Read a post with its rating summary, reviews and comments | ✔ | ✔ | ✔ |
| Comment on published posts; edit / delete own comments | ✔ | ✔ | ✔ |
| Rate (review) published posts of others; edit / delete own review | ✔ | ✔ | ✔ |
| Write posts, keep drafts, publish / unpublish, edit / delete **own** posts | – | ✔ | ✔ (any post) |
| See comments and reviews on own posts; delete (moderate) comments on own posts | – | ✔ | ✔ (all) |
| Tag own posts: pick from **own** tags or type new ones (created when the post is saved) | – | ✔ | ✔ (all tags) |
| Create / rename / delete tags on the Tags screen | – | – | ✔ |
| Manage users: create, edit, change role, delete | – | – | ✔ |
| Dashboard: totals, top-rated and most-commented posts, activity per user | – | – | ✔ |

Rules that protect the data: drafts are visible only to their author and admins; nobody can
review their own post; authors cannot delete reviews of their own posts (ratings stay honest);
an admin cannot delete or demote their own account; new visitors can register as Reader or
Author only. Admin is offered on the registration form only while the database has no admin, so
a fresh installation can create its first one.

### Sidebar per role

```
Reader                Author                    Admin
──────                ──────                    ─────
READ                  [ Write a post ]          [ Write a post ]
  Discover            READ                      OVERVIEW
MY ACTIVITY             Discover                  Dashboard
  My comments         MY BLOG                     Discover
  My reviews            My posts                MANAGE
                        Comments (on my posts)    Posts · Comments · Reviews
                        Reviews  (of my posts)    Tags · Users
```

---

## 2. How it works

```
 Splash ──(MySQL OK)──► Main window ──► Sign-in page ──(AuthService.login / register)──► Workspace
                         title bar       LoginController                                   ShellController
                         WindowFrame                                                       sidebar by role
                                                                                           top search bar
                                                                                           screens (BaseController)
                                                    ▼
                         Services check the Session before every change  ──►  DAO  ──►  MySQL
```

### 2.1 Roles are enforced in the service layer

Hiding a button is only a convenience. Every service receives the shared
[`Session`](../src/main/java/com/example/bloggingplatform/service/Session.java) (the signed-in
user) and checks it before acting, so a rule holds no matter which screen calls the service:

```java
public void delete(int id) {                                   // PostService
    Post existing = posts.getPostById(id);
    if (existing == null) {
        throw notFound();
    }
    session.requireOwnerOrAdmin(existing.getAuthorId(), "You can only delete your own posts.");
    ...
}

public Post create(Integer authorId, String title, ...) {
    User me = session.require();
    if (!me.getRole().canWritePosts()) {
        throw new AccessDeniedException("Only authors and admins can write posts. Readers can comment and rate.");
    }
    Integer author = me.getRole() == Role.ADMIN ? authorId : Integer.valueOf(me.getId());   // authors always write as themselves
    ...
}
```

`AccessDeniedException` extends `ValidationException`, so the UI shows a refused action exactly
like any other rule: as a message, never as a crash.

Reading is filtered the same way. `PostService.visible(filter)` returns the filter unchanged for
an admin or for an author listing their own posts; for everybody else it forces
`status = PUBLISHED`, so drafts never leak into Discover or search results.

| Service | Check |
|---------|-------|
| `PostService` | create: author/admin · update, publish, delete: owner or admin · read: published, or own, or admin |
| `CommentService` | create: anyone signed in, as themselves · edit: writer or admin · delete: writer, **post author** or admin |
| `ReviewService` | create: anyone signed in, as themselves, not on own post · edit/delete: reviewer or admin |
| `TagService` | create: author/admin · rename/delete: admin |
| `UserService` | everything admin-only; no self-delete, no self-demotion |
| `ReportService` | admin-only |

### 2.2 Sign in and registration (no new tables)

[`UserService`](../src/main/java/com/example/bloggingplatform/service/UserService.java) (`login`, `register`) uses the
existing columns: `users.username` / `users.email` to find the account
(`UserDAO.getUserByLogin`, one parameterized `WHERE username = ? OR email = ?`),
`users.password_hash` to check the password with the Phase 2 PBKDF2 hasher, and `users.role` to
decide what the user may do. A wrong username and a wrong password get the **same** message, so
the form does not reveal which accounts exist. Hashing deliberately takes a moment, so
[`LoginController`](../src/main/java/com/example/bloggingplatform/controller/LoginController.java)
runs it in a JavaFX `Task` on a background thread while the button reads "Signing in…".

### 2.3 New queries, still on the Phase 1 tables

| DAO method | SQL idea |
|------------|----------|
| `UserDAO.getUserByLogin`, `countUsersByRole` | `WHERE username = ? OR email = ?`, `COUNT(*) … WHERE role = ?` |
| `CommentDAO.getCommentsByUser`, `getCommentsOnPostsOf` | `WHERE c.user_id = ?`, `WHERE p.author_id = ?` (join to posts) |
| `ReviewDAO.getReviewsByUser`, `getReviewsOnPostsOf` | same for reviews |
| `ReportDAO.getSummary` | one row of `COUNT(*)` / `AVG(rating)` sub-queries: users per role, posts per status, comments, reviews |
| `ReportDAO.getUserActivity` | per user: posts, published, comments, reviews written, average rating received |

All of them are `PreparedStatement`s with bound `?` values, like the rest of the DAO layer.

### 2.4 The workspace

[`ShellController`](../src/main/java/com/example/bloggingplatform/controller/ShellController.java)
builds the sidebar from the role, loads a screen the first time it is opened (and keeps it), and
implements [`Navigator`](../src/main/java/com/example/bloggingplatform/controller/Navigator.java)
so screens can open a post, go back, or show one author's posts. Shared screens get a
[`Scope`](../src/main/java/com/example/bloggingplatform/controller/Scope.java): the same
`comments-view.fxml` is "Comments" (all, admin), "Comments on my posts" (author) or "My comments"
(reader).

The search bar always talks to the screen that is open (`BaseController.search(text)`). Discover
searches the in-memory search index by keyword, tag or author, ignoring case
([Phase 3](phase-3-searching-sorting-caching.md)). The Posts table searches in SQL (`LIKE ?` on
title and content, case-insensitive through the `_ai_ci` collation). The smaller lists
(comments, reviews, tags, users, dashboard activity) are searched in memory. Searching while
reading a post jumps back to Discover. Shortcuts: **Ctrl+F** search, **Ctrl+N** new record,
**F5** reload, **Esc** closes a pop-up.

| Screen | Controller | What is special |
|--------|-----------|-----------------|
| Discover | `FeedController` | Blog cards (author, date, reading time, excerpt, tag chips, ★ rating, comment count), 6 per page, tag/author filters; clicking a tag chip filters by it |
| Reading view | `ArticleController` | Middle: only the article. Right panel: rating summary with a 5→1 distribution and a row of stars (one click opens the rating pop-up with that many stars), then a Comments / Reviews switch; "Write a comment" opens a pop-up; edit/delete buttons only where allowed |
| My posts / All posts | `PostController` | Paged table (`LIMIT/OFFSET`), status/tag/author filters, row buttons Read · Edit · Publish/Unpublish · Delete, editor pop-up with a tag-chip input (see 2.7) |
| Comments / Reviews | `CommentController`, `ReviewController` | Scope-dependent title, rows and buttons |
| Tags, Users | `TagController`, `UserController` | Admin CRUD in pop-ups; Users can jump to a user's posts |
| Dashboard | `ReportController` | Four stat cards, top-rated and most-commented posts, per-user activity table |

### 2.5 Pop-ups and row buttons

`BaseController.openForm(title, form, creating, firstField, submit)` puts a screen's form (declared
in the FXML inside `<fx:define>`, so it is not on the page itself) into a
[`PopupWindow`](../src/main/java/com/example/bloggingplatform/util/PopupWindow.java) with **Cancel**
and a green **Create** or orange **Update** button. The pop-up stays open while saving fails;
the reason appears in a banner at the top of the form and under each wrong field
([`FormErrors`](../src/main/java/com/example/bloggingplatform/controller/FormErrors.java)).

Row buttons are declared once per table:

```java
rowActions(table, actionsColumn, this::edit,                       // double-click = edit
        viewAction("Read the post", post -> navigator.openPost(post.getId())),
        editAction(this::edit),
        new RowAction<>(post -> post.status() == PUBLISHED ? "Move back to drafts" : "Publish",
                Icons.PUBLISH, null, this::togglePublished, post -> true),
        deleteAction(this::delete));

editAction(this::edit).onlyWhen(comment -> app.comments().canEdit(comment))   // per-row visibility
```

They are compact icon buttons with tooltips: Edit is orange, Delete red, View and Publish black.

### 2.6 The window, the theme and the splash screen (`ui` package)

| Class | Job |
|-------|-----|
| `WindowFrame` | For the `StageStyle.TRANSPARENT` stage: drag by the title bar, double-click to maximize, resize from any edge or corner (event filters on the root), maximize to the screen's usable area so the taskbar stays visible, initial size = 86 % of the screen, centred |
| `PopupWindow` | Modal pop-up shell: rectangular white card, light title bar (drag to move; it stays on the screen), resizable from any edge, never larger than the screen (shrunk to fit, body scrolls), button row, Esc or ✕ to close |
| `Toast` | Success message: a floating white pill that slides up at the bottom of the window and fades out |
| `Icons` | SVG icons (Material Design paths) coloured by CSS |
| `NotebookArt` | The notebook and pen drawn with JavaFX shapes; the splash animation (a reveal clip widens behind the pen tip line by line); the logo and taskbar icons (snapshots of the drawing) |
| `SplashScreen` | Off-white card with the animation, the app name, a status line and three bouncing dots |
| `Theme` | Adds `app.css` to every scene |

**White first.** The theme follows a reference design the user supplied: white dominates and
black is used only for buttons and icons. The palette is defined once in
[`app.css`](../src/main/resources/com/example/bloggingplatform/view/app.css) as looked-up colours
and reused by every rule:

```css
.root {
    -paper: #fbfaf8;          /* main background (warm off-white) */
    -panel: #f4f2ef;          /* title bar, sidebar, table header */
    -tint: #efedea;           /* cards: posts, ratings, comments, dashboard stats */
    -tint-strong: #e6e3df;    /* selected pill, selected row */
    -line: #e3e0db;           /* hairline dividers */
    -border: #cdc8c2;         /* input outlines (black when focused) */
    -ink: #000000;            /* buttons, icons */
    -muted: #66625d;          /* secondary text */
}
.button { -fx-background-color: -ink; -fx-background-radius: 20; }   /* flat black pills */
```

The active sidebar entry and the current page number are black pills (like the active tab of a
bottom bar); the Comments / Reviews switch and selected table rows use a light grey pill.
Focus rings appear only for keyboard focus (`:focus-visible`). The only colours are still
**Create = green, Update = orange, Delete = red, Search = blue**.

**Two fonts**, both from Google Fonts under the SIL Open Font License (license texts next to the
files in `src/main/resources/com/example/bloggingplatform/fonts/`):

| Font | Used for | Where it is set |
|------|----------|-----------------|
| **Xmind** (sans-serif; Regular, SemiBold, Bold, Italic) | All text: buttons, menu, labels, forms, tables, posts, comments, reviews | `.root { -fx-font-family: "Xmind"; }` |
| **Zeyada** (handwriting) | Only a few titles: the app name (title bar, splash, sign-in page), page headings ("Discover", "All posts", "Create your account"), section headings ("Ratings", "Top rated posts") and pop-up titles | the "Fonts" block at the end of `app.css` |

JavaFX cannot use a web font `<link>`, and the app must work offline, so the TTF files are bundled
and registered once by [`Theme`](../src/main/java/com/example/bloggingplatform/util/Theme.java)
with `Font.loadFont` before any window is built; CSS then refers to them by family name.
Zeyada is thin and has no bold face, so its titles are set large at normal weight. In a JavaFX
label its line box is about 2.1 × the font size (Xmind: about 1.3 ×), so a negative
`-fx-label-padding` of `-0.42em` trims the extra space; the two-line brand title keeps its full
line box and pulls its lines together with a negative `-fx-line-spacing` instead. The sidebar
entries and the sign-in forms sit in scroll areas, so nothing can run off a short screen.

**Start-up** ([`Main`](../src/main/java/com/example/bloggingplatform/Main.java)):
the splash shows at once; the connection check runs with `CompletableFuture.supplyAsync` so the
animation keeps moving; after at least 2.6 s and a successful check the main window opens and
the splash fades out (the main window opens first, so the app never has zero windows and quits).
If MySQL cannot be reached, the splash stops and an error pop-up explains what to fix.

### 2.7 Tags: authors use their own tags

The `tags` table has no owner column, and the brief rules out schema changes, so **an author's
tags are the tags on that author's posts**: `TagDAO.getTagsUsedBy(authorId)` joins
`tags → post_tags → posts` on `posts.author_id`. `TagService.findForWriting()` returns those for
an author and every tag for an admin.

In the post editor the tags are a chip input: type a tag and press **Enter** (or a comma) to
turn it into a chip (`"Spring Boot"` → `#spring-boot`); click a chip's ✕ or press Backspace in
the empty field to remove one; "Your tags" below offers the author's own tags to click. Other
authors' tags are never offered.

Nothing touches the database until the post is saved. `PostService.create/update` receive the
tag **names**, validate them with the other fields, then look each one up by name or create it,
and save the post:

```java
public List<Tag> findOrCreate(List<String> names) {   // TagService, called by PostService
    List<Tag> result = new ArrayList<>();
    for (String name : names) {
        Tag tag = tags.getTagByName(name);
        if (tag == null) {
            int id = tags.addTag(new Tag(0, name));
            tag = new Tag(id, name);
        }
        result.add(tag);
    }
    return result;
}
```

All fields, the tags included, are checked before any tag is created, so a rejected post leaves
no stray tags.

Tag names are `UNIQUE` (3NF: one row per tag), so if an author types a name that already exists,
the existing tag is linked rather than a duplicate created; it then counts as one of that author's
tags as well. Creating, renaming and deleting tags directly is done by admins on the Tags screen.

---

## 3. Testing

`.\mvnw.cmd test` → **89 tests, 0 failures** ([saved output](testing/phase-2/junit-results.txt)).
New in this step:

| Test class | Tests | What it proves |
|-----------|------:|----------------|
| `AccessRulesTest` | 11 | Readers cannot write posts; authors write as themselves and cannot touch other authors' posts; drafts hidden from others; comment/review ownership; post authors may delete comments but not reviews on their posts; users, tag renames and reports are admin-only; admins cannot delete/demote themselves; nobody signed in cannot change anything; authors are offered only the tags of their own posts |
| `AuthServiceTest` | 5 | Sign in by username or email; same message for wrong user and wrong password; registration hashes and signs in; password confirmation; Admin only while no admin exists |
| `WindowFrameTest` | 3 | Edge/corner detection; resizing from the left keeps the right edge; minimum size respected |
| `SearchMatchTest` | 3 | Case-insensitive in-memory search; excerpt and reading-time helpers |
| `PostServiceTest` (new cases) | 2 | Typed tags are created on save and existing names reused; an invalid tag name rejects the post before anything is saved |
| `DAOIntegrationTest` (new case) | 1 | `getTagsUsedBy` returns only the tags on that author's posts; `getTagByName` |

The existing service tests now run with an admin session (`FakeDaos.adminSession()`), so they
still test the validation rules on their own.

**UI check.** A throw-away script signed in as a temporary reader, author and admin, opened
every sidebar screen, a post, the rating pop-up (by clicking the 4th star), the comment pop-up and the "Write a post" pop-up (typing two tags), saved snapshots of the app window,
and deleted the temporary accounts again. No exceptions were raised. Selected snapshots:
[`testing/phase-2/screenshots/`](testing/phase-2/screenshots/) (the temporary accounts had no
posts or comments of their own, so "My posts" and "My comments" show their empty state).

## 4. Known limitations

- One account is signed in per running app; there is no "remember me" and no password reset
  (both would need new tables or e-mail, which the brief rules out).
- Database calls still run on the JavaFX thread except sign-in/registration; with paged queries
  on a local server they take milliseconds.
- Window snapping with the Windows key (Win + arrows) does not apply to an undecorated window;
  maximize / restore are available from the title bar instead.

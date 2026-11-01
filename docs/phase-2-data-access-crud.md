# Phase 2 — Data Access & CRUD Operations (JavaFX + JDBC)

| | |
|---|---|
| **Phase** | 2 — Data Access & CRUD ([phases.md](../phases.md)) |
| **User stories** | 2.1 (CRUD through JavaFX with validation), 2.2 (dynamic listing with pagination, filters, search) |
| **Stack** | Java 21 (runs on newer JDKs) · JavaFX 21 · JDBC with `mysql-connector-j` 9.1 · MySQL 8.0 · JUnit 5 |
| **Entry point** | `mvnw javafx:run` → [`Main`](../src/main/java/com/example/bloggingplatform/Main.java) |
| **Evidence** | [`testing/phase-2/junit-results.txt`](testing/phase-2/junit-results.txt): 142 tests, 0 failures · [`testing/phase-2/screenshots/`](testing/phase-2/screenshots/) |
| **UI & roles** | Screens, theme and role-based sign-in: [`ui-and-roles.md`](ui-and-roles.md) · strict input validation: [`input-validation.md`](input-validation.md) |

---

## 1. What was built

A desktop application that manages the Phase 1 tables: **users, posts, comments, reviews and
tags** (plus `post_tags`, see the [ERD](db%20erd.png)). Users sign in, and what they see depends
on their role:
- **Readers** browse published posts as blog cards, read them, comment and rate.
- **Authors** can also write, publish and manage their own posts.
- **Admins** manage every table and see a dashboard.

Every table has Create / Update / Delete through pop-up forms and buttons in each row. The post
listings load one page at a time from MySQL and can be filtered by status, tag and author or
searched by keyword.

### Architecture

```
 JavaFX UI       controller/ + FXML screens      what the user sees and clicks
     │
     ▼
 Service layer   service/                        business rules, roles and validation
     │
     ▼
 DAO layer       dao/                            JDBC queries (PreparedStatement only)
     │
     ▼
 Model classes   model/                          POJOs: User, Post, Comment, Tag, Review
     │
     ▼
 MySQL 8.0       config/DBConnection             URL, USER, PASSWORD → DriverManager
```

Each layer only calls the one below it: controllers never write SQL, and DAOs never check
roles.

### Code structure

```
src/main/java/com/example/bloggingplatform/
│
├── config/
│   └── DBConnection.java       the MySQL URL, user and password, and getConnection()
│
├── model/                POJO classes (private fields, constructors, getters and setters)
│   ├── User.java         a reader, an author or an admin
│   ├── Post.java         a blog post (+ its tags, comment count and average rating)
│   ├── Comment.java      a comment on a post
│   ├── Tag.java          a post category / tag
│   ├── Review.java       a 1-5 star rating with optional text
│   └── Role, PostStatus (enums) · PostFilter, Page, SiteSummary, UserActivity (query helpers)
│
├── dao/                  what can be read and written, as interfaces (one per table)
│   ├── UserDAO.java, PostDAO.java, CommentDAO.java, TagDAO.java, ReviewDAO.java, ReportDAO.java
│   ├── DaoException.java
│   └── impl/             the MySQL versions, in plain JDBC
│       └── UserDAOImpl.java, PostDAOImpl.java, CommentDAOImpl.java, TagDAOImpl.java, ReviewDAOImpl.java, ReportDAOImpl.java
│
├── service/
│   ├── AuthService.java  sign in, register, sign out
│   ├── UserService.java  the Users screen (admins manage accounts)
│   ├── PostService.java, CommentService.java, ReviewService.java, TagService.java, ReportService.java
│   ├── Session.java      the signed-in user
│   ├── AccessDeniedException.java   a role rule was broken
│   └── AppContext.java   creates the DAOs and services and connects them (the wiring)
│
├── validations/          every input rule, one class per subject
│   ├── UserValidator.java      username, email, password, sign-in
│   ├── PostValidator.java      title, content
│   ├── CommentValidator.java, ReviewValidator.java, TagValidator.java
│   ├── TextRules.java          small text checks the validators share
│   ├── Validation.java         collects the problems of one form
│   └── ValidationException.java   one message per form field
│
├── controller/
│   ├── LoginController.java    sign-in and registration page
│   ├── PostController.java     the posts table (My posts / All posts)
│   ├── FeedController.java     Discover (post cards)
│   ├── ArticleController.java  the reading view
│   ├── CommentController, ReviewController, TagController, UserController, ReportController
│   ├── MainController, ShellController (window and sidebar)
│   └── BaseController (shared screen behaviour), RowActions (table row buttons),
│       FormErrors, Dialogs, BlogWidgets, CommentForm, … (small helpers)
│
├── util/
│   ├── PasswordHasher.java     PBKDF2 password hashes
│   └── Theme, Icons, PopupWindow, Toast, WindowFrame, SplashScreen, NotebookArt (UI helpers)
│
└── Main.java                   starts the app
```

The screens themselves are FXML files in `src/main/resources/com/example/bloggingplatform/view/`,
styled by `app.css`.

### SOLID principles

| Principle | Where it is applied |
|-----------|---------------------|
| **S**ingle responsibility: a class has one reason to change | DAOs only run SQL, services only hold rules, controllers only drive screens. `AuthService` signs users in while `UserService` manages accounts. `TagService` owns creating tags, and `PostService` asks it for them. Each subject has its own validator (`UserValidator`, `PostValidator`, …). `RowActions` builds the table-row buttons, so `BaseController` keeps only the shared screen behaviour. `BlogWidgets` builds the small UI pieces and `CommentForm` is the comment pop-up. |
| **O**pen / closed: add behaviour without editing working code | Another database (e.g. MongoDB for comments in Phase 4) is a new class implementing `CommentDAO`, with no change to `CommentService`. A new rule goes into its validator. A new screen extends `BaseController` and gets forms, toasts and error handling for free. A screen adds its own row button (the posts table's *Publish*) with `RowActions.Action`, without changing `RowActions`. |
| **L**iskov substitution: an implementation can stand in for its interface | The services work the same with `PostDAOImpl` (MySQL) and with the in-memory `FakeDaos.Posts` used by the unit tests. Both implement `PostDAO` fully. `AccessDeniedException` can go wherever a `ValidationException` is expected: the screens show it the same way. |
| **I**nterface segregation: small, focused interfaces | One DAO interface per table, so `CommentService` sees only what it needs. The read-only dashboard queries have their own `ReportDAO` instead of growing `PostDAO`. Screens talk to the window through the 3-method `Navigator` interface, not the whole `ShellController`. |
| **D**ependency inversion: depend on abstractions | Services receive DAO **interfaces** in their constructors and never create a JDBC class. `AppContext.create()` is the only place that picks the MySQL implementations (`new PostDAOImpl()`) and passes them in (constructor injection). Controllers receive the services from `AppContext`. |

```java
public class PostService {
    private final PostDAO posts;       // an interface: MySQL in the app, an in-memory fake in tests
    private final TagService tags;
    ...
    public PostService(PostDAO posts, TagService tags, UserDAO users, Session session) { ... }
}

// AppContext.create(): the only place that knows the MySQL classes
PostDAO postDAO = new PostDAOImpl();
PostService posts = new PostService(postDAO, tagService, userDAO, session);
```


## 2. Requirements → Implementation

| # | Requirement | How it is met | Main files |
|---|-------------|---------------|-----------|
| 2.1-a | All CRUD operations work through JavaFX | One screen per entity: a table with Edit / Delete buttons in every row, a green "New …" button, and pop-up forms. Which screens and buttons appear depends on the signed-in role. | `controller/*Controller.java`, `view/*-view.fxml` |
| 2.1-b | Input validation and user feedback | Services check every field with the validators in `validations/` and report all errors at once. The form shows each message in red under its field and checks fields live; a success shows a toast. | `validations/*Validator.java`, `validations/Validation.java`, `controller/FormErrors.java` |
| 2.1-c | DB constraints prevent duplicates and invalid entries | Services check duplicates first (username, email, post title per author, tag name, one review per user and post). The Phase 1 UNIQUE / FK / CHECK constraints stay as the last line; `DaoException` puts their errors into plain words. | `service/*Service.java`, `dao/DaoException.java` |
| 2.2-a | Post listings retrieved dynamically from the DB | `PostService.findPage` → `PostDAO.getPostsPage`: one `SELECT … LIMIT ? OFFSET ?` per page | `service/PostService.java`, `dao/impl/PostDAOImpl.java` |
| 2.2-b | Pagination, filters and search | JavaFX `Pagination`, filters for status, tag and author, case-insensitive keyword search on title and content | `controller/PostController.java`, `controller/FeedController.java`, `model/PostFilter.java`, `model/Page.java` |
| 2.2-c | Parameterized queries via JDBC | Every DAO method uses a `PreparedStatement` with `?` placeholders; no user value is ever put into the SQL text | `dao/impl/*DAOImpl.java` |
| T-APP2 | JDBC driver | `mysql-connector-j` in `pom.xml`, `requires java.sql` in `module-info.java`, plain `DriverManager.getConnection` | `config/DBConnection.java` |
| T-APP3 | Layered architecture | `model` · `dao` (+ `dao.impl`) · `service` · `validations` · `controller` · `config` · `util`; services depend on DAO interfaces, and all wiring is in `AppContext.create()` | `service/AppContext.java` |
| D-3 | Runnable JavaFX application | `mvnw javafx:run`, or run `Main` from the IDE | `Main.java` |
| D-6 | Testing evidence | 142 JUnit tests (16 against real MySQL), screenshots | §6 |

## 3. How to run

**Prerequisites:** a JDK (21 or newer), MySQL 8.0 running locally, and the schema from
[`sql/01_schema.sql`](sql/01_schema.sql). Maven is not needed: the wrapper `mvnw` downloads it.

1. **Configure the connection.** Open `src/main/java/com/example/bloggingplatform/config/DBConnection.java`
   and set `URL`, `USER` and `PASSWORD` for your MySQL. (Do not push a real password to a public
   repository.)
2. **Start the app** from the project root with `.\mvnw.cmd javafx:run`, or run
   `com.example.bloggingplatform.Main` in IntelliJ.
3. **Run the tests** with `.\mvnw.cmd test`. The MySQL tests are skipped, not failed, when the
   database is unreachable.

If the database is down or misconfigured, the splash screen stops and one pop-up explains it
(e.g. *"Could not connect to the database. MySQL refused the login: check USER and PASSWORD in
config/DBConnection.java."*), then the app exits.

## 4. Feature tour

| Screen | Who | Content | Notes |
|--------|-----|---------|-------|
| **Discover** | everyone | Published posts as cards, 6 per page, newest first | Search bar (title and content), tag / author filters; a card opens the reading view with ratings, reviews and comments |
| **Posts** / **My posts** | admin / author | title, author, status, tags, comments, rating, updated | Status / tag / author filters, "Showing 11-20 of 37 posts"; Read · Edit · Publish · Delete per row |
| **Comments** | admin, author (on own posts), reader (own) | post, writer, text, date | Filter by post; only published posts accept comments |
| **Reviews** | admin, author (of own posts, read-only), reader (own) | post, reviewer, ★ rating, text, date | One review per user per post, no self-review |
| **Tags** | admin | id, name | Names become slugs (`" Spring Boot "` → `spring-boot`) |
| **Users** | admin | username, email, role, joined | Passwords hashed with PBKDF2; a blank password on Update keeps the current one |
| **Dashboard** | admin | totals, top posts, activity per user | Counted by SQL `COUNT` / `AVG` |

Every delete asks for confirmation, with *Cancel* focused so that Enter never deletes.

---

## 5. Code walkthrough

### 5.1 Model — POJO classes

Each table row is a plain Java object: private fields, a no-argument constructor, a constructor
with all the fields, and a getter and setter per field. For example `Post`:

```java
public class Post {
    private int id;                 // 0 until the post is saved
    private int authorId;
    private String authorName;      // users.username, read from a join
    private String title;
    private String content;
    private PostStatus status;
    private List<Tag> tags = new ArrayList<>();
    private int commentCount;       // counted by the query
    private Double averageRating;   // null when the post has no reviews
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Post() {}
    public Post(int id, int authorId, String title, String content, PostStatus status, List<Tag> tags) { … }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    // … the other getters and setters
}
```

`User`, `Comment`, `Tag` and `Review` follow the same pattern. Two objects with the same `id`
are equal (`equals` / `hashCode` compare the id), which is what the drop-down lists need.
`Role` and `PostStatus` are enums that mirror the MySQL `ENUM` columns; `Role.canWritePosts()`
encodes "only AUTHOR and ADMIN write posts".

The query helpers are small read-only classes: `PostFilter` (status, author, tag, keyword;
`null` means "any"), `Page<T>` (one page of rows plus the total), and `SiteSummary` /
`UserActivity` for the dashboard.

### 5.2 Connecting — `config/DBConnection`

```java
public class DBConnection {

    public static final String URL = "jdbc:mysql://localhost:3306/bloggingPlatform";
    public static final String USER = "root";
    public static final String PASSWORD = "change-me";   // put your MySQL password here

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}
```

Every DAO method opens a connection with it and closes it with try-with-resources. For a
single-user desktop app on a local database this is fast enough, and it needs no connection
pool. `Main` opens one connection at start-up, so a wrong password shows up straight away.

### 5.3 DAO layer — `dao/` and `dao/impl/`

Each table has an interface in `dao/` (what can be read and written) and a MySQL version in
`dao/impl/` written in plain JDBC. Each public method is complete on its own: the SQL, the
`PreparedStatement`, and a loop that turns each `ResultSet` row into a POJO. For example
`PostDAOImpl.getAllPosts()`:

```java
@Override
public List<Post> getAllPosts() {
    List<Post> posts = new ArrayList<>();
    String sql = SELECT_POSTS + " ORDER BY p.title";
    try (Connection conn = DBConnection.getConnection();
         PreparedStatement ps = conn.prepareStatement(sql);
         ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
            posts.add(readPost(rs));
        }
        for (Post post : posts) {
            post.setTags(getTagsOfPost(conn, post.getId()));   // the tags come from post_tags
        }
    } catch (SQLException e) {
        throw new DaoException("Could not load the posts.", e);
    }
    return posts;
}

/** Turns the current row into a Post object (without its tags). */
private Post readPost(ResultSet rs) throws SQLException {
    Post post = new Post();
    post.setId(rs.getInt("post_id"));
    post.setAuthorId(rs.getInt("author_id"));
    post.setAuthorName(rs.getString("author_name"));
    post.setTitle(rs.getString("title"));
    post.setContent(rs.getString("content"));
    post.setStatus(PostStatus.valueOf(rs.getString("status")));
    …
    return post;
}
```

1. JDBC reads the rows from MySQL.
2. Each row becomes a `Post` object, filled with its setters.
3. The objects go into a `List<Post>`.
4. The service checks who may see them, and the controller shows them in a card list or a
   `TableView`.

The method names say what they do: `getAllPosts`, `getPostById` (returns `null` when there is
no such post), `getPostsPage`, `countPosts`, `addPost`, `updatePost`, `deletePost`, and the same
for the other tables (`getAllUsers`, `addComment`, `getReviewsByPost`, `getTagByName`, …).
`DaoException` turns an `SQLException` into a message the screen can show.

**No SQL injection.** The SQL text only ever contains `?` placeholders, and every value is
bound with `setInt` / `setString`. Typed text can never change the meaning of a query. The
integration test `keywordIsBoundAsDataNotAsSql` searches for `' OR '1'='1` and finds nothing.

**Writing.** `addX` uses `Statement.RETURN_GENERATED_KEYS` to get the new `AUTO_INCREMENT` id.
`updateX` and `deleteX` return `false` when the row no longer exists.

**Transactions.** A post and its tag links must be saved together, so `PostDAO.addPost` and
`updatePost` turn off auto-commit, write the post and the `post_tags` rows, and commit. If
anything fails they roll back:

```java
try (Connection conn = DBConnection.getConnection()) {
    conn.setAutoCommit(false);
    try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
        … INSERT INTO posts … → id
        addTagLinks(conn, id, post.getTags());   // INSERT INTO post_tags … per tag
        conn.commit();
        return id;
    } catch (SQLException e) {
        conn.rollback();   // nothing of the post is saved
        throw e;
    }
}
```

The test `failedTagLinkRollsBackThePostInsert` links a tag that does not exist and checks that
no post row is left behind.

**Listing, filters, search and pagination (`PostDAO.getPostsPage`).** The SQL starts with
`WHERE 1=1`, and one `AND …` is added for each filter that is set. The values go into a list in
the same order as their `?` marks:

```java
StringBuilder sql = new StringBuilder(" WHERE 1=1");
if (filter.getStatus() != null) {
    sql.append(" AND p.status = ?");
    values.add(filter.getStatus().name());
}
…
if (filter.getKeyword() != null) {
    sql.append(" AND (p.title LIKE ? OR p.content LIKE ?)");
    String pattern = "%" + escapeLike(filter.getKeyword()) + "%";
    values.add(pattern);
    values.add(pattern);
}
… + " ORDER BY p.created_at DESC, p.post_id DESC LIMIT ? OFFSET ?"
```

- **Case-insensitive.** MySQL 8's default collation `utf8mb4_0900_ai_ci` ignores case and
  accents, so `LIKE '%java%'` also finds "Java".
- **Literal wildcards.** `escapeLike` escapes `%`, `_` and `\`, so a search for `100%` finds
  exactly that text.
- **Stable paging.** `post_id` breaks ties in the order, so no post appears on two pages.
- **Derived values.** The comment count and average rating are counted by subqueries, not
  stored (database design §5.4).
- `countPosts(filter)` uses the same `WHERE`, so "Showing x-y of N" always matches the pages.

**`DaoException`** wraps the `SQLException` and adds plain words for the common MySQL errors:

| MySQL code | Message added |
|-----------|---------------|
| 1062 | "A record with the same value already exists." |
| 1451 / 1452 | "A related record is missing or still in use." |
| 1045 | "MySQL refused the login: check USER and PASSWORD in config/DBConnection.java." |
| 1049 | "The database does not exist: run docs/sql/01_schema.sql first." |
| SQLSTATE `08…` | "Cannot reach the MySQL server: check that it is running." |

### 5.4 Service layer — `service/` and `validations/`

The services hold the **business rules** and check every input **before** anything reaches the
database. The rules live in the `validations` package, one validator per subject
(`UserValidator`, `PostValidator`, `CommentValidator`, `ReviewValidator`, `TagValidator`).
`Validation` collects all problems of one form, so the user sees every bad field after one click,
and `ValidationException` carries one message per field:

```java
Validation v = new Validation();
v.required("author", "Author", authorId);
String cleanTitle = v.text("title", title, PostValidator::title);   // 3-200 characters, …
v.text("content", content, PostValidator::content);                // at least 20 characters
…
if (!v.hasError("title") && posts.isTitleTaken(authorId, cleanTitle, postId)) {
    v.reject("title", "This author already has a post with that title.");
}
v.throwIfInvalid();   // ValidationException(message, {field → message, …})
```

The rules themselves are described in [`input-validation.md`](input-validation.md). The
rules that need other tables are checked by the services:

| Service | Rules |
|---------|-------|
| `AuthService` | Sign in by username or email (the same message for a wrong name and a wrong password); register as Reader or Author (Admin only while no admin exists). |
| `UserService` | Username and email must be free. A blank password on update keeps the old hash. Only admins manage accounts; an admin cannot delete themselves or remove their own admin role. |
| `PostService` | Only authors and admins write; an author always writes as themselves. The author must exist and be allowed to write. One title per author. Typed tags that do not exist yet are created (by `TagService.findOrCreate`). A page index past the end moves back to the last page. Drafts are visible only to their author and admins. |
| `CommentService` | Only on published posts. Writers edit their own comments; the post's author and admins may delete them. |
| `ReviewService` | Rating 1-5, only on published posts, no review of your own post, one review per user per post. |
| `TagService` | Names become slugs and must be free. Only admins manage tags. |
| `ReportService` | Dashboard numbers, admins only. |

**Roles** are checked in the services through `Session` (the signed-in user), so the rules hold
no matter which screen calls them. A refused action throws `AccessDeniedException`.

**Passwords — `util/PasswordHasher`.** Passwords are stored as salted PBKDF2-HMAC-SHA256 with
600,000 iterations (the OWASP recommendation), built into the JDK. Only `UserService` handles
plain passwords; the DAO never sees one.

**Wiring — `AppContext.create()`** is the one place that picks the MySQL implementations of the
DAO interfaces and connects them to the services:

```java
UserDAO userDAO = new UserDAOImpl();           // interface type, MySQL class
PostDAO postDAO = new PostDAOImpl();
TagDAO tagDAO = new TagDAOImpl();
...
Session session = new Session();               // the signed-in user, shared by all services
PasswordHasher hasher = new PasswordHasher();
UserService userService = new UserService(userDAO, hasher, session);
TagService tagService = new TagService(tagDAO, session);
return new AppContext(session,
        new AuthService(userDAO, userService, hasher, session),
        userService,
        new PostService(postDAO, tagService, userDAO, session),
        new CommentService(commentDAO, postDAO, userDAO, session),
        …);
```

Controllers receive the `AppContext` and can only reach services, never a DAO.

### 5.5 JavaFX UI — `Main`, controllers and views

**Start-up (`Main`).** A splash screen shows while the database connection is checked on a
background thread. Then the main window opens on the sign-in page. Every view is loaded by
[`Views.load`](../src/main/java/com/example/bloggingplatform/controller/Views.java) with a
controller factory, so controllers are built with `new XController(app)`.

**`LoginController`** shows the sign-in and registration forms and calls `AuthService.login` /
`register`. **`ShellController`** builds the sidebar for the user's role and opens the screens.

**`BaseController`** holds the error handling shared by every screen, so the screens never
write `try/catch`:

```java
protected <T> Optional<T> save(FormErrors target, String successMessage, Supplier<T> action) {
    target.clear();
    try {
        T saved = action.get();
        Dialogs.toast(window(), successMessage);
        return Optional.of(saved);
    } catch (ValidationException e) {         // a rule broken in the service (incl. roles)
        target.show(e.getMessage(), e.fieldErrors());
    } catch (DaoException e) {                // a problem in the database
        target.show(e.getMessage(), Map.of());
    }
    return Optional.empty();
}
```

**`PostController`** (the posts table): the JavaFX `Pagination` control is the page navigator,
and the rows appear in the `TableView` above it. Picking a page runs one `LIMIT/OFFSET` query
and updates the table and the "Showing x-y of N" label. The search bar or a filter box builds
a new `PostFilter` and goes back to page 1.

### 5.6 One request end to end: "Create post"

```
Author clicks "Write a post", fills the pop-up, clicks Create
 └─ PostController            → BaseController.save(...)   shows errors in red, or a toast
     └─ PostService.create(authorId, title, content, status, tags)
         ├─ Session: signed in as AUTHOR or ADMIN
         ├─ Validation + PostValidator: every field; title not used twice by this author
         ├─ TagService.findOrCreate: an existing tag or a new one for each typed tag
         └─ PostDAO.addPost(post)                     (interface; PostDAOImpl in the app)
              conn.setAutoCommit(false)
              INSERT INTO posts (...) VALUES (?, ?, ?, ?)       → generated post_id
              INSERT INTO post_tags (post_id, tag_id) VALUES (?, ?)   × number of tags
              conn.commit()   (rollback on any error)
         ← PostDAO.getPostById(id): the saved post with author name, tags and dates
     ← "Your post was saved." toast, pop-up closed, list reloaded
```

---

## 6. Testing evidence

Command: `.\mvnw.cmd test`. Saved output:
[`testing/phase-2/junit-results.txt`](testing/phase-2/junit-results.txt).
**Result: 142 tests, 0 failures, 0 errors, 0 skipped** (2026-10-30, local MySQL 8.0).

| Test class | Kind | Tests | What it proves |
|-----------|------|------:|----------------|
| `DAOIntegrationTest` | Integration (real MySQL) | 16 | CRUD round trips for every DAO. UNIQUE keys stop a duplicate username (ignoring case) and a second review. The CHECK constraint stops a rating outside 1-5. The foreign key stops a comment on a missing post. A failed tag link rolls back the post insert. Every filter works, alone and combined. Pages are newest first and do not overlap. A keyword is bound as data (`' OR '1'='1` finds nothing). `%` / `_` match literally. Deleting a post cascades to comments and reviews but keeps the tag. Deleting a user cascades to their posts. An author's tags are the tags on their posts. |
| `UserValidatorTest`, `PostValidatorTest`, `CommentValidatorTest`, `ReviewValidatorTest`, `TagValidatorTest` | Unit | 53 | Every input rule: email regex, password strength, username, title, content, comment, review, rating, tags ([`input-validation.md`](input-validation.md)) |
| `PostServiceTest` | Unit (fake DAOs) | 12 | Trimming, duplicate tags removed, errors per field, length limits, readers cannot write, unknown author, one title per author, page clamping |
| `ReviewServiceTest` | Unit | 10 | Rating range and required, no self-review, no reviews on drafts, blank text → `NULL`, one review per user, average |
| `UserServiceTest` | Unit | 9 | Only a hash is stored, every invalid field at once, username format, username and email must be free, keep or replace the password |
| `AuthServiceTest` | Unit | 5 | Sign in by username or email; the same message for a wrong user and a wrong password; registration; Admin only while no admin exists |
| `TagServiceTest` | Unit | 8 | Slugs, invalid characters, blank or too long, names must be free |
| `AccessRulesTest` | Unit | 11 | Every role rule (readers cannot write, authors only manage their own posts, admin-only screens, …) |
| `CommentServiceTest` | Unit | 5 | Trimmed save, no comments on drafts, required fields, edit changes only the text |
| `DaoExceptionTest` | Unit | 3 | MySQL error codes → plain words |
| `PasswordHasherTest`, `WindowFrameTest`, `SearchMatchTest`, `PostDAOImplTest` | Unit | 10 | Hashing, window resizing, the screens' search filter, `escapeLike` |

The integration test gives its rows a unique prefix and deletes them afterwards (deleting the
test users cascades to everything they own), so existing data is left as it was. The unit tests
give each service in-memory implementations of the DAO interfaces
([`FakeDaos`](../src/test/java/com/example/bloggingplatform/service/FakeDaos.java)), so the
service rules are tested without a database. This works because the services depend only on the
interfaces (dependency inversion).

**UI check.** A scripted run signed in as a temporary reader, author and admin, opened every
screen, the reading view, the rating, comment and "Write a post" pop-ups and a search, without
errors. The screenshots are in [`testing/phase-2/screenshots/`](testing/phase-2/screenshots/).

## 7. Known limitations and next steps

- **No connection pool.** Each query opens its own connection: fine for one user on a local
  database. Phase 4 will measure it.
- **Database calls run on the JavaFX thread** (except sign-in and registration). With paged
  queries on a local server they take milliseconds; a remote database would need a JavaFX
  `Task`.
- **Tags are loaded with one small query per post.** That is simple and fine for a page of 6-10
  posts; a very long list would load them with one `IN (…)` query instead.
- **Keyword search uses `LIKE '%…%'`,** which cannot use a B-tree index. Faster search and
  caching are Phase 3.

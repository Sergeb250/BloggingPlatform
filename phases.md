# Smart Blogging Platform — Project Phases

The project is delivered in **5 phases**, one per epic. Every requirement from the brief
(user-story acceptance criteria, technical requirements, DSA requirements and project
deliverables) is assigned to exactly one phase and mapped to the concrete deliverable that
satisfies it. All design documents live in [`docs/`](docs).

**Stack:** MySQL 8.0 · Java + JavaFX 21 · JDBC · (MongoDB for Phase 4 NoSQL work) · JUnit 5

**Status legend:** ✅ Done · 🟡 In progress / needs evidence · ⬜ Not started

| Phase | Title | Epic | User stories | Points it targets | Status |
|-------|-------|------|--------------|-------------------|--------|
| 1 | Database Design & Modeling | Epic 1 | 1.1, 1.2 | Database Design (25), SQL Implementation (20) | ✅ |
| 2 | Data Access & CRUD (JavaFX + JDBC) | Epic 2 | 2.1, 2.2 | JavaFX + JDBC Integration (20) | ✅ |
| 3 | Searching, Sorting & Caching | Epic 3 | 3.1, 3.2 | DSA Application (15) | ✅ |
| 4 | Performance, Query Optimization & NoSQL | Epic 4 | 4.1, 4.2 | Performance Optimization (10), SQL/NoSQL (20) | 🟡 |
| 5 | Reporting & Documentation | Epic 5 | 5.1 | Documentation & Code Quality (10) | ✅ |

---

## Phase 1 — Database Design & Modeling

**Goal:** a normalized (3NF) MySQL schema for users, posts, comments, tags and reviews, with
keys, indexes and referential integrity, documented at conceptual, logical and physical level.

### Requirements → Deliverables

| # | Requirement (source) | Deliverable | Location | Status |
|---|----------------------|-------------|----------|--------|
| 1.1-a | Conceptual ERD includes all major entities and relationships (US 1.1) | ERD diagram + conceptual model section | [`docs/db erd.png`](docs/db%20erd.png), [`docs/database-design.md` §3](docs/database-design.md#3-conceptual-model) | ✅ |
| 1.1-b | Logical model defines attributes, PK/FK and relationships (US 1.1) | Logical model section (per-table attribute/key tables) | [`docs/database-design.md` §4](docs/database-design.md#4-logical-model) | ✅ |
| 1.1-c | Physical model with SQL data types, constraints, normalization to 3NF (US 1.1) | Physical model + normalization sections; DDL script | [`docs/database-design.md` §5–6](docs/database-design.md#5-normalization-1nf--3nf), [`docs/sql/01_schema.sql`](docs/sql/01_schema.sql) | ✅ |
| 1.2-a | Appropriate primary and foreign keys defined (US 1.2) | PK/FK constraints in DDL | [`docs/sql/01_schema.sql`](docs/sql/01_schema.sql) | ✅ |
| 1.2-b | Indexes on frequently queried columns — post title, author ID, tag name (US 1.2) | Index catalogue + index DDL | [`docs/database-design.md` §7](docs/database-design.md#7-index-design), [`docs/sql/01_schema.sql`](docs/sql/01_schema.sql) | ✅ |
| 1.2-c | Referential integrity enforced (US 1.2) | FK `ON DELETE / ON UPDATE` rules + CHECK/UNIQUE/NOT NULL constraints | [`docs/database-design.md` §8](docs/database-design.md#8-referential-integrity--constraints) | ✅ |
| T-DB1 | MySQL / PostgreSQL / MongoDB (Tech req.) | MySQL 8.0, InnoDB, utf8mb4 | [`docs/sql/01_schema.sql`](docs/sql/01_schema.sql) | ✅ |
| T-DB2 | Normalized to 3NF (Tech req.) | Functional-dependency analysis per table | [`docs/database-design.md` §5](docs/database-design.md#5-normalization-1nf--3nf) | ✅ |
| T-DB3 | Entities: Users, Posts, Comments, Tags, Reviews (Tech req.) | 5 entity tables + `post_tags` junction | [`docs/sql/01_schema.sql`](docs/sql/01_schema.sql) | ✅ |
| T-DB4 | Indexes on frequently queried fields (Tech req.) | See 1.2-b | — | ✅ |
| T-DB5 | Referential integrity enforced (Tech req.) | See 1.2-c | — | ✅ |
| D-1 | Database Design Document (Deliverable) | Design document | [`docs/database-design.md`](docs/database-design.md) | ✅ |
| D-2 | SQL Implementation Script (Deliverable) | Schema + seed data scripts | [`docs/sql/01_schema.sql`](docs/sql/01_schema.sql) | ✅ |
| D-6 | Testing Evidence (Deliverable, Phase 1 part) | Constraint/integrity/index verification script + saved output | `DAOIntegrationTest` (keys, cascades, CHECK and UNIQUE rules on the real database); catalogue check output in the README, US 1.2 | ✅ |

### Exit criteria
- `01_schema.sql` runs cleanly on MySQL 8.0.
- `DAOIntegrationTest` proves every key and constraint on the real database.

---

## Phase 2 — Data Access & CRUD Operations (JavaFX + JDBC)

**Goal:** a layered JavaFX application (Controller → Service → DAO) that performs all CRUD
operations through parameterized JDBC queries, plus a reader view with pagination, filters and search.

**Write-up:** [`README.md`](README.md) — what was built, how to run it, layer-by-layer code walkthrough, testing evidence.
UI redesign and role-based access: [`README.md`](README.md).

### Code structure

```
src/main/java/com/example/bloggingplatform/
├── model/        POJO classes: User, Post, Comment, Tag, Review (+ Role, PostStatus, PostFilter, Page, SiteSummary, UserActivity)
├── dao/          interfaces UserDAO, PostDAO, CommentDAO, TagDAO, ReviewDAO, ReportDAO; DaoException
│   └── impl/     their MySQL versions: UserDAOImpl, PostDAOImpl, … (plain JDBC)
├── service/      AuthService, UserService, PostService, CommentService, ReviewService, TagService, ReportService,
│                 Session, AccessDeniedException, AppContext
├── validations/  UserValidator, PostValidator, CommentValidator, ReviewValidator, TagValidator, TextRules,
│                 Validation, ValidationException (every input rule, one class per subject)
├── controller/   LoginController, PostController, FeedController, ArticleController, CommentController,
│                 ReviewController, TagController, UserController, ReportController, ShellController, … (+ helpers)
├── config/       DBConnection (MySQL URL, user, password)
├── util/         PasswordHasher, and the UI helpers (Theme, Icons, PopupWindow, Toast, WindowFrame, SplashScreen, NotebookArt)
└── Main.java
```

JavaFX UI (controllers + FXML screens) → Service layer (business rules and validation) → DAO layer
(JDBC queries) → Model classes → MySQL.

### Requirements → Deliverables

Paths are relative to `src/main/java/com/example/bloggingplatform/` unless they start with `src/`, `docs/` or `pom.xml`.

| # | Requirement (source) | Deliverable | Location | Status |
|---|----------------------|-------------|----------|--------|
| 2.1-a | All CRUD operations functional via JavaFX (US 2.1) | Posts, Comments, Reviews, Tags and Users screens: table with Edit/Delete in each row, green "New" button, pop-up forms; screens shown per role | `controller/*Controller.java`, `src/main/resources/.../view/*-view.fxml` | ✅ |
| 2.1-b | Input validation and user feedback (US 2.1) | Strict rules in the validators (`UserValidator`, `PostValidator`, …: email regex, password with letter + number + symbol, username, lengths, …) applied by every service, with all field errors reported at once (`Validation`, `ValidationException`). The forms check the same rules live; messages, outlines and the banner are red (`FormErrors`). Success toasts and error pop-ups (`Dialogs`). Write-up: [`README.md`](README.md) | `validations/InputRules.java`, `validations/`, `controller/FormErrors.java` | ✅ |
| 2.1-c | DB constraints prevent duplicates / invalid entries (US 2.1) | The services check duplicates first (username, email, post title per author, tag name, one review per user and post) and show the message under the field; the Phase 1 UNIQUE / FK / CHECK constraints stay as the last line, and `DaoException` turns their errors into plain words | `service/*Service.java`, `dao/DaoException.java` | ✅ |
| 2.2-a | Post listings dynamically retrieved from DB (US 2.2) | `PostService.findPage` → `PostDAO.getPostsPage` feeding the Discover cards and the posts `TableView` | `dao/impl/PostDAOImpl.java`, `controller/PostController.java`, `controller/FeedController.java` | ✅ |
| 2.2-b | Pagination, filters and search (US 2.2) | `LIMIT ? OFFSET ?` paging + JavaFX `Pagination`; filters by status/tag/author; case-insensitive keyword search on title and content (`LIKE`) | `model/PostFilter.java`, `model/Page.java`, `dao/impl/PostDAOImpl.java` | ✅ |
| 2.2-c | Parameterized queries via JDBC (US 2.2) | Every DAO method uses a `PreparedStatement` with bound `?` values; SQL-injection test passes | `dao/impl/*DAOImpl.java`, `src/test/.../dao/DAOIntegrationTest.java` | ✅ |
| T-APP1 | JavaFX UI for CRUD, search, analytics (Tech req.) | CRUD + search screens (analytics screen follows in Phase 4) | `controller/`, `src/main/resources/.../view/` | ✅ |
| T-APP2 | JDBC driver with parameterized queries (Tech req.) | `mysql-connector-j` 9.1, plain `DriverManager.getConnection` in `DBConnection` | `pom.xml`, `config/DBConnection.java` | ✅ |
| T-APP3 | Layered architecture Controller → Service → DAO (Tech req.) | Packages `model`, `dao`, `service`, `validations`, `controller`, `config`, `util`; all wiring in `AppContext.create` | `service/AppContext.java`, `Main.java`; SOLID notes in the Phase 2 write-up | ✅ |
| D-3 | JavaFX Application (Deliverable, core) | Runnable via `mvnw javafx:run`, or `Main` from the IDE | `Main.java` | ✅ |
| D-6 | Testing Evidence (Phase 2 part) | 142 JUnit tests (16 MySQL integration + 126 unit), 0 failures; UI screenshots of every role | [`docs/screenshots/`](docs/screenshots/) | ✅ |
| X-1 | Sign in, register and role-based access (Reader / Author / Admin) on the existing `users` table (user request) | `AuthService.login` / `register`, `Session` checks in every service, role-specific sidebar | `service/AuthService.java`, `service/Session.java`, `controller/ShellController.java` | ✅ |
| X-2 | Blog-style reading for readers, writing workspace for authors, admin dashboard with reports (user request) | Discover cards, reading view with ratings and comments, My posts with publish, `ReportDAO` aggregate queries | `controller/FeedController.java`, `ArticleController.java`, `ReportController.java`, `dao/impl/ReportDAOImpl.java` | ✅ |
| X-3 | White-first UI (black only on buttons and icons; Xmind for all text, handwritten Zeyada for a few titles): sidebar, top search bar, row action buttons, pop-up forms, own window controls, bordered window, notebook-and-pen splash (user request) | UI helpers in `util` and `app.css` | `util/Theme.java`, `util/PopupWindow.java`, …, `view/app.css` | ✅ |

### Setup tasks (done)
- ✅ Added `com.mysql:mysql-connector-j`, raised compiler `release` to 21, added `requires java.sql;` to `module-info.java`.
- ✅ Replaced the `Hello*` template classes with `Main`.

---

## Phase 3 — Searching, Sorting & Caching (DSA)

**Goal:** fast, case-insensitive search by keyword, tag or author; in-memory caching with
correct invalidation; sorting/searching algorithms applied to real data.

**Write-up:** [`README.md`](README.md) — what was built, code walkthrough (merge sort, binary search, search index, cache, invalidation), index ↔ Java structure mapping with Big-O, before / after timings, testing evidence, limitations.

### Requirements → Deliverables

| # | Requirement (source) | Deliverable | Location | Status |
|---|----------------------|-------------|----------|--------|
| 3.1-a | Case-insensitive search (US 3.1) | One search box finds posts by any word of the title, content, tags or author name; words are stored and looked up in lower case, and may be typed partly | `cache/SearchIndex.java`, `controller/FeedController.java` | ✅ |
| 3.1-b | Indexing or hashing improves search performance (US 3.1) | Inverted index `HashMap<String, Set<Integer>>` (word → post ids) | `cache/SearchIndex.java` | ✅ |
| 3.1-c | Performance improvements documented (US 3.1) | Before / after timings: search 134-3,456× faster on 10,000 posts; Discover from cache ~1,000× faster than MySQL | `docs/phase-3-searching-sorting-caching.md` §6, `docs/screenshots/` | ✅ |
| 3.2-a | In-memory caching using Maps or Lists (US 3.2) | `PostCache`: published posts in a `List` (newest first) and a `HashMap` by id, plus the search index | `cache/PostCache.java` | ✅ |
| 3.2-b | Sorting/search algorithms applied (US 3.2) | Hand-written merge sort (Discover's Sort box, index words) and binary search (words typed partly) | `algorithms/MergeSort.java`, `algorithms/BinarySearch.java`, `model/PostSort.java` | ✅ |
| 3.2-c | Cache invalidation ensures consistency (US 3.2) | Every change to a post or to what a post shows calls `cache.clear()`; the next read reloads | `service/*Service.java`, `PostCacheTest`, `CacheIntegrationTest` | ✅ |
| T-DSA1 | Hashing for fast lookup | See 3.1-b; also `HashMap<Integer, Post>` for opening a post | `cache/PostCache.java` | ✅ |
| T-DSA2 | Sorting and searching algorithms | See 3.2-b | `algorithms/` | ✅ |
| T-DSA3 | Indexing concept mapping | Database index types ↔ Java structures, with Big-O | `docs/phase-3-searching-sorting-caching.md` §5 | ✅ |
| T-APP4 | In-memory caching (Maps, Lists) | See 3.2-a | `cache/PostCache.java` | ✅ |
| D-6 | Testing Evidence (Phase 3 part) | 39 new tests (181 in total, 0 failures), timings, screenshots | `docs/screenshots/` | ✅ |

---

## Phase 4 — Performance, Query Optimization & NoSQL

**Goal:** prove the optimizations with numbers, add an analytics view, and model
comments/reviews as NoSQL documents with a justified SQL vs NoSQL comparison.

**Write-ups:** [`README.md`](README.md) (indexes, benchmark, caching, analytics) and [`README.md`](README.md) (MongoDB design, justification, SQL vs NoSQL comparison).

### Requirements → Deliverables

| # | Requirement (source) | Deliverable | Location | Status |
|---|----------------------|-------------|----------|--------|
| 4.1-a | Query execution times recorded before/after optimization (US 4.1) | Bulk data in a separate database (100,000 posts) + Java harness (3 warm-up runs, median of 15) timing the app's queries without and with the indexes, plus `EXPLAIN ANALYZE` | `src/test/resources/benchmark/`, `src/test/.../perf/QueryBenchmarkTest.java` | ✅ |
| 4.1-b | Indexing and caching demonstrate measurable gains (US 4.1) | Report: Discover page 307 → 1.7 ms (179×), title order 71×, My posts 5×; cache hit ~1,400× vs MySQL; tables, chart, plans, costs, the `LIKE` counter-example | `README.md` US 4.1 | ✅ |
| 4.2-a | NoSQL schema defined for unstructured data (US 4.2) | MongoDB `comments` / `reviews` / `counters` with `$jsonSchema` validators, indexes, sample documents and example queries | `docs/nosql/mongo-schema.js` | 🟡 script written; run output to save |
| 4.2-b | Justification provided (US 4.2) | Why comments/reviews, embedding vs referencing, snapshots, replies, flexible fields | `README.md` US 4.2 | ✅ |
| 4.2-c | Optional SQL vs NoSQL comparison (US 4.2) | Comparison table; `CommentMongoDAO` implements `CommentDAO`, and the unchanged `CommentService` passes 4 integration tests on MongoDB Atlas | `README.md` US 4.2, `dao/mongo/CommentMongoDAO.java`, `config/MongoConnection.java` | ✅ |
| T-DSA4 | Performance measurement before/after optimization | See 4.1-a / 4.1-b | `README.md` US 4.1 | ✅ |
| T-APP1 | JavaFX UI … analytics (Tech req., analytics part) | The admin Dashboard: top-rated posts, most commented, activity per author, **posts per tag** (bars) and **cache statistics** (hit rate, hits, misses, load time) | `controller/ReportController.java`, `ReportDAO.getPostsPerTag`, `PostCache.getStats` | ✅ |
| D-2 | SQL / NoSQL Implementation Script (NoSQL part) | Mongo setup script (+ SQL: `01_schema.sql`) | `docs/nosql/mongo-schema.js`, `docs/sql/` | 🟡 run the Mongo script |
| D-4 | Performance Report (Deliverable) | Report | `README.md` US 4.1 | ✅ |
| D-6 | Testing Evidence (Phase 4 part) | Benchmark output, `EXPLAIN ANALYZE` plans, JUnit results (188 tests), dashboard screenshots | `docs/screenshots/` | ✅ |

---

## Phase 5 — Reporting & Documentation

**Goal:** a maintainable, well-documented, submission-ready project.

### Requirements → Deliverables

| # | Requirement (source) | Deliverable | Location | Status |
|---|----------------------|-------------|----------|--------|
| 5.1-a | ERDs or schema diagrams included (US 5.1) | ERD image; architecture (layer) diagram and MongoDB document diagram in Mermaid | `docs/db erd.png`, `README.md` §2 and US 4.2 | ✅ |
| 5.1-b | SQL/NoSQL scripts provided (US 5.1) | Ordered scripts 01-05 (schema, seed, verify, indexes, bulk data) + `mongo-schema.js`, with a run guide; 01→02→03 tested on a fresh database (12/12 PASS) | `docs/sql/`, `docs/nosql/`, `README.md` §4 | ✅ |
| 5.1-c | README with setup instructions (US 5.1) | Setup, demo accounts, architecture, every user story with its acceptance criteria and proof (screenshots or code) | [`README.md`](README.md) | ✅ |
| D-5 | README File (Deliverable) | See 5.1-c | `README.md` | ✅ |
| D-6 | Testing Evidence (Deliverable, consolidated) | Evidence index linking test results, screenshots, timings, benchmark output, plans and the verify output | [`README.md`](README.md) | ✅ |
| Q-1 | Documentation & Code Quality (Eval. criteria) | Javadoc on public classes and methods, one structure (model / dao / service / controller), no template code left, IntelliJ inspections clean on the new code | whole codebase | ✅ |
| Q-2 | Final submission check | Every row in this file marked ✅ | `phases.md` | 🟡 waiting for the MongoDB script run (4.2-a, D-2) |

---

## Traceability Summary

### Project deliverables
| Deliverable | Phase(s) | Final location |
|-------------|----------|----------------|
| Database Design Document | 1 (finalized in 5) | `docs/database-design.md` |
| SQL / NoSQL Implementation Script | 1 (SQL), 4 (NoSQL) | `docs/sql/`, `docs/nosql/` |
| JavaFX Application | 2, 3, 4 | `src/` |
| Performance Report | 4 | `README.md` US 4.1 |
| README File | 5 | `README.md` |
| Testing Evidence | 1–4 (consolidated in 5) | `docs/screenshots/` |

### Learning objectives
| Objective | Phase |
|-----------|-------|
| Design and normalize a schema for the blogging domain | 1 |
| Conceptual, logical and physical models | 1 |
| CRUD and complex queries with SQL and JDBC | 2 |
| Indexing, caching, searching and sorting algorithms | 1 (indexes), 3 |
| Integrate DB operations into JavaFX | 2 |
| Compare SQL and NoSQL for unstructured data | 4 |
| Measure and document performance improvement | 4 |

### Evaluation criteria
| Criterion | Points | Phase(s) |
|-----------|--------|----------|
| Database Design | 25 | 1 |
| SQL / NoSQL Implementation | 20 | 1, 4 |
| JavaFX + JDBC Integration | 20 | 2 |
| DSA Application | 15 | 3 |
| Performance Optimization | 10 | 4 |
| Documentation & Code Quality | 10 | 5 |
| **Total** | **100** | |

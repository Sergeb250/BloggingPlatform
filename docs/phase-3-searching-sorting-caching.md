# Phase 3 — Searching, Sorting & Caching (DSA)

**Goal:** fast, case-insensitive search by keyword, tag or author; an in-memory cache that is
always correct; a sorting and a searching algorithm applied to real data.

**Result:** the Discover screen now reads published posts from memory instead of MySQL. The
search bar finds posts by any word of the title, content, tags or author name, ignoring case and
accepting words typed partly. A new **Sort** box orders the cards with a hand-written merge sort.
On the real data a Discover page is served about **1,000× faster** than the Phase 2 SQL query,
and opening a post about **27,000× faster** (section 6).

---

## 1. What was built

```
Discover screen (FeedController)
   │  search text, tag, author, sort
   ▼
PostService.discover(filter, sort, page, size)
   │
   ├─► PostCache ──── empty? ──► PostDAO.getAllPosts() ──► MySQL      (cache miss: once)
   │     List<Post>  published, newest first                          (cache hit: memory)
   │     HashMap<Integer, Post>  byId
   │     SearchIndex: HashMap<String, Set<Integer>> word → post ids
   │                  List<String> sortedWords  (for binary search)
   │
   ├─► keep posts whose id the index found, then the author / tag filters
   ├─► MergeSort.sort(matches, sort.comparator())
   └─► one page of 6 cards
```

Every service method that changes a post, or something shown on one, ends with
`cache.clear()`. The next read loads fresh data, so the screens never show old values.

| New / changed | What it does |
|---------------|--------------|
| `algorithms/MergeSort.java` | Stable O(n log n) merge sort, used for Discover's sort orders and the index's word list |
| `algorithms/BinarySearch.java` | `firstAtLeast(sortedWords, key)`: O(log n) position of the first word ≥ key |
| `cache/SearchIndex.java` | Inverted index: every lower-case word → ids of the posts containing it; prefix search with binary search |
| `cache/PostCache.java` | Published posts in a `List` and a `HashMap` by id, plus the search index; `clear()` empties it |
| `model/PostSort.java` | The four orders of the Sort box (newest, oldest, title A-Z, top rated) as comparators |
| `service/PostService.java` | New `discover(...)`; `findById` reads published posts from the cache; writes clear the cache |
| `service/CommentService`, `ReviewService`, `TagService`, `UserService` | Clear the cache after a change that a post shows |
| `service/AppContext.java` | Creates the one shared `PostCache` and gives it to the services |
| `controller/FeedController.java`, `view/feed-view.fxml` | Uses `discover(...)`; new Sort box; prompt "Search posts by keyword, tag or author" |

No table was added and the schema did not change.

## 2. Requirements → Implementation

| # | Requirement | How it is met | Where |
|---|-------------|---------------|-------|
| 3.1-a | Case-insensitive search | Words are stored and looked up in lower case (`Locale.ROOT`), so `JAVA`, `Java` and `java` give the same result. Tag names and author names are indexed too, so one box searches by keyword, tag or author | `cache/SearchIndex.java` |
| 3.1-b | Indexing or hashing improves search | Inverted index `HashMap<String, Set<Integer>>`: a word is found in O(1) instead of reading every post | `cache/SearchIndex.java` |
| 3.1-c | Performance documented | Before / after timings for search (10,000 posts) and for the cache (real MySQL data) | §6, [`testing/phase-3/timings.txt`](testing/phase-3/timings.txt) |
| 3.2-a | In-memory caching with Maps or Lists | `List<Post>` of published posts, `HashMap<Integer, Post>` by id, the index's `HashMap` | `cache/PostCache.java` |
| 3.2-b | Sorting / search algorithms applied | Merge sort orders Discover (Sort box) and the word list; binary search finds words typed partly | `algorithms/` |
| 3.2-c | Invalidation keeps data consistent | Every change shown on a post calls `cache.clear()` (table in §4.6); tested per method and end to end on MySQL | services, `PostCacheTest`, `CacheIntegrationTest` |
| T-DSA1 | Hashing for fast lookup | `HashMap` word → ids and id → post | `SearchIndex`, `PostCache` |
| T-DSA2 | Sorting and searching algorithms | `MergeSort`, `BinarySearch` | `algorithms/` |
| T-DSA3 | Indexing concept mapping | Database index types ↔ Java structures with Big-O | §5 |
| T-APP4 | In-memory caching | See 3.2-a | `cache/PostCache.java` |
| D-6 | Testing evidence | 39 new tests (181 in total), timings, screenshots | §7 |

## 3. How to try it

Run the app (`.\mvnw.cmd javafx:run`), sign in and open **Discover**:

- type `JAVA` or `java`: the same posts (case is ignored);
- type `ind`: posts with "index" or "indexes" (a word can be typed partly);
- type `carol`: Carol's posts (author name); `mysql`: posts tagged or mentioning MySQL;
- type `java mysql`: only posts with both words;
- choose **Sort → Top rated**: highest average rating first, unrated posts last;
- add a comment or a rating, go back to Discover: the card shows the new count or average
  straight away (the cache was emptied and reloaded).

Screenshots: [`testing/phase-3/screenshots/`](testing/phase-3/screenshots/).

## 4. Code walkthrough

### 4.1 Merge sort — `algorithms/MergeSort.java`

Split the list in two halves, sort each half (recursively), then merge them: repeatedly take the
smaller front item of the two halves.

```java
public static <T> void sort(List<T> list, Comparator<? super T> order) {
    if (list.size() < 2) {
        return;   // 0 or 1 item is already sorted
    }
    int middle = list.size() / 2;
    List<T> left = new ArrayList<>(list.subList(0, middle));
    List<T> right = new ArrayList<>(list.subList(middle, list.size()));
    sort(left, order);
    sort(right, order);
    merge(left, right, list, order);
}
```

- **Time O(n log n)** in every case (the list is halved log n times; each level merges n items).
- **Memory O(n)** for the two halves.
- **Stable:** `merge` takes from the left half when items are equal (`<= 0`), so equal items keep
  their order.

Why merge sort: its time does not depend on the input (no O(n²) worst case like quicksort), and
stability means a later sort never shuffles posts that tie.

### 4.2 Binary search — `algorithms/BinarySearch.java`

```java
public static int firstAtLeast(List<String> sorted, String key) {
    int low = 0;
    int high = sorted.size();   // the answer is always between low and high
    while (low < high) {
        int middle = (low + high) / 2;
        if (sorted.get(middle).compareTo(key) < 0) {
            low = middle + 1;   // the middle word is too small: the answer is to its right
        } else {
            high = middle;      // the middle word is big enough: the answer is here or to its left
        }
    }
    return low;
}
```

It returns the position of the first word that is equal to or after `key` in A-Z order.
Example: `[api, java, javafx, mysql]` with `"jav"` gives 1. All words starting with `jav` sit
together from there on (`java`, `javafx`), because the list is sorted.
**O(log n):** 10,000 words take at most 14 steps.

### 4.3 The search index — `cache/SearchIndex.java`

Built once from the published posts:

```java
for (Post post : posts) {
    for (String word : words(textOf(post))) {       // title, content, tags, author name
        if (!postsByWord.containsKey(word)) {
            postsByWord.put(word, new HashSet<>());
        }
        postsByWord.get(word).add(post.getId());     // word → ids of posts that contain it
    }
}
sortedWords = new ArrayList<>(postsByWord.keySet());
MergeSort.sort(sortedWords, Comparator.naturalOrder());
```

`words(text)` lower-cases the text and splits it at everything that is not a letter or digit:
`"Spring-Boot 3!"` → `spring`, `boot`, `3`.

To search, each query word is looked up as a word **prefix**:

```java
int i = BinarySearch.firstAtLeast(sortedWords, prefix);
while (i < sortedWords.size() && sortedWords.get(i).startsWith(prefix)) {
    ids.addAll(postsByWord.get(sortedWords.get(i)));   // hash map lookup: O(1)
    i++;
}
```

With several query words, only posts that match **all** of them are kept (`retainAll`).
A query with no letters or digits (for example `!!`) finds nothing.

### 4.4 The cache — `cache/PostCache.java`

```java
private List<Post> published;        // newest first; null means "not loaded" (empty cache)
private Map<Integer, Post> byId;     // post id → post, so one post is found in O(1)
private SearchIndex index;           // word → post ids, for the search bar
```

- `getPublished()`, `getPublished(id)` and `search(text)` first call `loadIfEmpty()`.
  - **Miss:** if `published` is null, it loads every post with `PostDAO.getAllPosts()`, keeps the
    published ones, sorts them newest first with merge sort, and builds `byId` and the index.
  - **Hit:** otherwise it returns straight away.
- `clear()` sets all three to null.
- Drafts are never cached; `PostService.findById` reads them from the database.
- `published` is assigned last. If MySQL fails while loading, the cache stays empty and the
  next read simply tries again (tested).

### 4.5 Discover — `PostService.discover` and `model/PostSort`

```java
Set<Integer> found = null;   // null: no search text, every post matches
if (filter.getKeyword() != null) {
    found = cache.search(filter.getKeyword());
}
for (Post post : cache.getPublished()) {
    boolean wanted = (found == null || found.contains(post.getId()))
            && (filter.getAuthorId() == null || filter.getAuthorId() == post.getAuthorId())
            && (filter.getTagId() == null || hasTag(post, filter.getTagId()));
    if (wanted) {
        matches.add(post);
    }
}
MergeSort.sort(matches, sort.comparator());
```

Then it cuts out one page. A page number past the end moves back to the last page, as in
Phase 2.

`PostSort` gives each order of the Sort box a comparator:

| Order | Rule |
|-------|------|
| Newest first | `created_at` descending; same second: higher id first |
| Oldest first | the reverse |
| Title A-Z | `compareToIgnoreCase`; ties newest first |
| Top rated | average rating descending; unrated posts last; ties newest first |

### 4.6 Cache invalidation

The rule: **every change to a post, or to something a post shows (author name, tags, comment
count, average rating), ends with `cache.clear()`.**

| Service method | Clears? | Why |
|----------------|:-------:|-----|
| `PostService.create / update / changeStatus / delete` | ✅ | the post itself |
| `CommentService.create / delete` | ✅ | comment count |
| `CommentService.update` | — | the comment text is not part of a post |
| `ReviewService.create / update / delete` | ✅ | average rating |
| `TagService.rename / delete` | ✅ | tag names on posts |
| `TagService.create` | — | a new tag is on no post yet |
| `UserService.update` | ✅ | author name |
| `UserService.delete` | ✅ | the user's posts are deleted (cascade) |
| `UserService.create`, `AuthService.register` | — | a new user has no posts |

`clear()` is called only after the database call succeeded. A change that failed validation
changes nothing and keeps the cache.

## 5. Indexing concept mapping (T-DSA3)

How the indexes a database uses relate to Java data structures (n = rows / items, k = matches):

| Database index | Example here | Java structure | Find one | Insert | Prefix / range / order |
|----------------|--------------|----------------|----------|--------|------------------------|
| Clustered B+tree primary key | `posts.post_id` | `TreeMap<Integer, Post>` | O(log n) | O(log n) | yes, in key order |
| Unique secondary B-tree | `uq_users_username`, `uq_tags_name` | `TreeMap<String, Integer>` (or `HashSet` if only "exists?") | O(log n) | O(log n) | yes |
| Composite B-tree (leftmost prefix) | `uq_posts_author_title (author_id, title)` | `TreeMap<Integer, TreeMap<String, Post>>` | O(log n) | O(log n) | by `author_id`, then `title` |
| Hash index (InnoDB adaptive hash; `MEMORY` tables) | built by InnoDB itself | `HashMap` — `PostCache.byId`, `SearchIndex.postsByWord` | **O(1)** average | O(1) | no order |
| FULLTEXT (inverted index) | planned `ft_posts_title_content` | `Map<String, Set<Integer>>` — `SearchIndex` | O(1) per word + k | O(words of the post) | no |
| B-tree scan for `LIKE 'jav%'` | an index on `title` | sorted `List<String>` + binary search — `SearchIndex.sortedWords` | O(log n + k) | O(n) to keep sorted | yes |
| No usable index (`LIKE '%jav%'`) | Phase 2 keyword search | a `for` loop over a `List` (`scanEveryPost` in the timing test) | O(n × text length) | O(1) | — |

The schema has the primary keys and the unique keys. The secondary and FULLTEXT indexes are
designed in [`database-design.md`](database-design.md) (§ indexes) and still open for Phase 1 and
Phase 4.

## 6. Performance: before and after (3.1-c)

Measured by `PerformanceTimingsTest` (average after a warm-up, Java 27, MySQL 8.0 on
localhost). Full output: [`testing/phase-3/timings.txt`](testing/phase-3/timings.txt). Times
depend on the computer; the ratios are what matter.

**Search** over 10,000 generated posts (about 60 words each). *Before:* check every post's text.
*After:* the search index. The test also asserts that both find exactly the same posts.

| Query | Matches | Check every post | Search index | Faster |
|-------|--------:|-----------------:|-------------:|-------:|
| `w7x` | 294 | 19,061 µs | 50.8 µs | 375× |
| `w1234x` | 285 | 15,216 µs | 27.8 µs | 548× |
| `w42x w99x` (both words) | 4 | 14,498 µs | 108.0 µs | 134× |
| `nothing` (no match) | 0 | 12,371 µs | 3.6 µs | 3,456× |

Building the index for 10,000 posts takes about 0.47 s, once per cache load. For the 13 real
posts the whole miss (MySQL load plus index) takes 23 ms.

**Cache** on the real MySQL data (13 published posts). *Before:* Phase 2 queried MySQL each time.
*After:* a cache hit.

| Operation | MySQL (Phase 2) | Cache hit | Faster |
|-----------|----------------:|----------:|-------:|
| Discover, first page | 38,975 µs | 28.0 µs | 1,394× |
| Discover, search "java" | 32,091 µs | 32.6 µs | 983× |
| Open one post (find by id) | 12,296 µs | 0.5 µs | 27,324× |
| Cache miss (load every post + build the index) | — | 22,751 µs | paid once after each change |

**Reading the numbers:**
- The index wins because a lookup costs the same however many posts there are, while the scan
  grows with every post.
- The cache wins because it skips the network round trip, the SQL work and building objects
  from rows.
- The price is one reload after each change, which is still faster than one Phase 2 page.

## 7. Testing evidence (D-6)

Command: `.\mvnw.cmd test`. Saved output: [`testing/phase-3/junit-results.txt`](testing/phase-3/junit-results.txt).
**Result: 181 tests, 0 failures, 0 errors, 0 skipped** (2026-11-01, local MySQL 8.0). Phase 3
added 39 tests:

| Test class | Kind | Tests | What it proves |
|-----------|------|------:|----------------|
| `MergeSortTest` | Unit | 5 | Empty and one-item lists; unsorted, reversed and sorted input; the comparator decides the order; equal items keep their order (stable); same result as Java's sort on 200 random lists |
| `BinarySearchTest` | Unit | 5 | Empty list; exact word; prefix gives the first word starting with it; missing words before, between and after; agrees with a linear scan for every key, with duplicates |
| `SearchIndexTest` | Unit | 8 | Case is ignored; partly typed words; every query word must match; found by tag and author; punctuation splits words; accents; no match / only punctuation / empty index find nothing |
| `PostCacheTest` | Unit (fake DAOs) | 11 | Loaded once, then served from memory; only published posts, newest first; `clear()` reloads; a failed load is retried. Every changing method in the table of §4.6 empties the cache, and the three that should not keep it. Publishing and deleting show up on Discover |
| `PostServiceTest` (7 new) | Unit (fake DAOs) | 7 | Discover: published only, newest first; search ignores case and finds partial words; found by tag name; author and tag filters; all four sort orders; paging and clamping; empty result; `findById` from cache and drafts from the database |
| `CacheIntegrationTest` | Integration (real MySQL) | 1 | End to end through the real services: after adding a comment and a review, renaming the tag, renaming the author and deleting the post, Discover shows the new count, average, tag name and author name (also found by the new name in capitals), then nothing |
| `PerformanceTimingsTest` | Timing | 2 | Produces §6; asserts that the index and the full scan find the same posts |

**UI check.** A scripted run signed in as a temporary reader (deleted afterwards). It searched
`JAVA`, `ind` and `carol` and chose *Top rated*; the screenshots are in
[`testing/phase-3/screenshots/`](testing/phase-3/screenshots/).

## 8. Known limitations

- **One app instance.** The cache is emptied by changes made through this app. A row changed
  directly in MySQL (e.g. in Workbench), or by a second copy of the app, shows up only after the
  next change made in this app, or a restart.
- **Whole cache, cleared whole.** Any change reloads every published post. That is simple and
  always correct; with tens of thousands of posts it would be better to update only the changed
  post.
- **Prefix matching only.** `boot` finds "spring-boot" (split into `spring`, `boot`), but `oot`
  finds nothing: words match from their start, as in most search boxes. Phase 2's `LIKE '%…%'`
  found any substring.
- **Cached posts are shared objects.** Screens only read them; code that changed a cached
  `Post` would change what every screen shows (none does).
- **The tables still use SQL.** The Posts, Comments and Reviews tables (with drafts, for authors
  and admins) still page in MySQL as in Phase 2. Only Discover and the reading view use the cache.

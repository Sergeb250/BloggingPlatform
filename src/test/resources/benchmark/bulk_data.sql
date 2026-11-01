-- Bulk data for QueryBenchmarkTest, in a SEPARATE database (the real one is not touched).
-- Copies the tables of bloggingPlatform with CREATE TABLE ... LIKE and fills them with:
--   1,000 users, 30 tags, 100,000 posts, 200,000 tag links, 200,000 comments, 100,000 reviews.
-- Rows are generated with a recursive CTE: seq produces the numbers 1, 2, 3, ... n.

DROP DATABASE IF EXISTS bloggingPlatform_perf;
CREATE DATABASE bloggingPlatform_perf;
USE bloggingPlatform_perf;

CREATE TABLE users     LIKE bloggingPlatform.users;
CREATE TABLE posts     LIKE bloggingPlatform.posts;
CREATE TABLE tags      LIKE bloggingPlatform.tags;
CREATE TABLE post_tags LIKE bloggingPlatform.post_tags;
CREATE TABLE comments  LIKE bloggingPlatform.comments;
CREATE TABLE reviews   LIKE bloggingPlatform.reviews;

SET SESSION cte_max_recursion_depth = 1000000;

-- 1,000 users: user1..user100 are authors, the rest readers
INSERT INTO users (username, email, password_hash, role)
WITH RECURSIVE seq (n) AS (SELECT 1 UNION ALL SELECT n + 1 FROM seq WHERE n < 1000)
SELECT CONCAT('user', n), CONCAT('user', n, '@example.com'), REPEAT('x', 64), IF(n <= 100, 'AUTHOR', 'READER')
FROM seq;

-- 30 tags: tag-1 .. tag-30
INSERT INTO tags (name)
WITH RECURSIVE seq (n) AS (SELECT 1 UNION ALL SELECT n + 1 FROM seq WHERE n < 30)
SELECT CONCAT('tag-', n) FROM seq;

-- 100,000 posts by the 100 authors; every 5th is a draft; dates spread over ~70 days in mixed order
INSERT INTO posts (author_id, title, content, status, created_at, updated_at)
WITH RECURSIVE seq (n) AS (SELECT 1 UNION ALL SELECT n + 1 FROM seq WHERE n < 100000)
SELECT (n % 100) + 1,
       CONCAT('Benchmark post ', n),
       CONCAT('Post number ', n, ' about topic ', n % 50, '. ', REPEAT('Lorem ipsum dolor sit amet. ', 10)),
       IF(n % 5 = 0, 'DRAFT', 'PUBLISHED'),
       NOW() - INTERVAL ((n * 7919) % 100000) MINUTE,
       NOW() - INTERVAL ((n * 7919) % 100000) MINUTE
FROM seq;

-- 2 tags per post (two neighbouring tags, so never the same one twice)
INSERT INTO post_tags (post_id, tag_id)
WITH RECURSIVE seq (n) AS (SELECT 1 UNION ALL SELECT n + 1 FROM seq WHERE n < 100000)
SELECT n, (n % 30) + 1 FROM seq
UNION ALL
SELECT n, ((n + 1) % 30) + 1 FROM seq;

-- 200,000 comments by the 900 readers, 2 per post on average
INSERT INTO comments (post_id, user_id, content, created_at)
WITH RECURSIVE seq (n) AS (SELECT 1 UNION ALL SELECT n + 1 FROM seq WHERE n < 200000)
SELECT (n % 100000) + 1, (n % 900) + 101, CONCAT('Comment number ', n, '. Nice post!'),
       NOW() - INTERVAL ((n * 104729) % 100000) MINUTE
FROM seq;

-- 100,000 reviews: one per post, by a reader (never the author), rating 1 to 5
INSERT INTO reviews (post_id, user_id, rating, review_text, created_at)
WITH RECURSIVE seq (n) AS (SELECT 1 UNION ALL SELECT n + 1 FROM seq WHERE n < 100000)
SELECT n, (n % 900) + 101, (n % 5) + 1, IF(n % 2 = 0, CONCAT('Review number ', n), NULL),
       NOW() - INTERVAL ((n * 104729) % 100000) MINUTE
FROM seq;

-- fresh statistics, so the optimizer chooses plans for the real table sizes
ANALYZE TABLE users, posts, tags, post_tags, comments, reviews;

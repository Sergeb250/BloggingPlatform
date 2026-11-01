-- Phase 4 indexes, created by QueryBenchmarkTest in the benchmark database
-- (docs/sql/01_schema.sql creates the same three in the real database).

-- Discover and the post tables: WHERE status = 'PUBLISHED' ORDER BY created_at DESC LIMIT ?, ?
CREATE INDEX idx_posts_status_created ON posts (status, created_at);

-- "My posts": WHERE author_id = ? ORDER BY created_at DESC
CREATE INDEX idx_posts_author_created ON posts (author_id, created_at);

-- Lists in title order: ORDER BY title
CREATE INDEX idx_posts_title ON posts (title);

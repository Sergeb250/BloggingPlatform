// =====================================================
// Blogging Platform - NoSQL part (Phase 4): comments and reviews as MongoDB documents
// =====================================================
// Run with mongosh (your own connection string, e.g. from MongoDB Atlas):
//   mongosh "mongodb+srv://<user>:<password>@cluster0.78weq9k.mongodb.net/" --file docs/nosql/mongo-schema.js
// It DROPS and re-creates the collections, then loads sample documents, so run it on a database
// you can reset. MySQL stays the main database; see docs/nosql-design.md for the reasons.
//
// Numbers are written with NumberInt(...) because mongosh would otherwise store them as doubles,
// and the validators below require whole numbers ("int"), like the Java driver writes them.

db = db.getSiblingDB("bloggingPlatform");

// ---------- comments: one document per comment ----------
// The post title, the post's author and the writer's name are copied into the comment, so
// reading the comments of a post needs no join. Replies live inside their comment.
db.comments.drop();
db.createCollection("comments", {
  validator: {
    $jsonSchema: {
      bsonType: "object",
      required: ["_id", "postId", "post", "user", "content", "createdAt"],
      properties: {
        _id:      { bsonType: "int", minimum: 1, description: "comment id, handed out by the counters collection" },
        postId:   { bsonType: "int", minimum: 1, description: "posts.post_id in MySQL" },
        post: {
          bsonType: "object",
          required: ["title", "authorId"],
          properties: {
            title:    { bsonType: "string", minLength: 3, maxLength: 200 },
            authorId: { bsonType: "int", minimum: 1 }
          }
        },
        user: {
          bsonType: "object",
          required: ["id", "username"],
          properties: {
            id:       { bsonType: "int", minimum: 1 },
            username: { bsonType: "string", minLength: 3, maxLength: 50 }
          }
        },
        content:   { bsonType: "string", minLength: 2, maxLength: 2000 },
        createdAt: { bsonType: "date" },
        editedAt:  { bsonType: "date", description: "optional: only comments that were edited have it" },
        replies: {
          bsonType: "array",
          maxItems: 100,
          description: "optional: threaded replies, stored inside the comment",
          items: {
            bsonType: "object",
            required: ["user", "content", "createdAt"],
            properties: {
              user: {
                bsonType: "object",
                required: ["id", "username"],
                properties: { id: { bsonType: "int" }, username: { bsonType: "string" } }
              },
              content:   { bsonType: "string", minLength: 2, maxLength: 2000 },
              createdAt: { bsonType: "date" }
            }
          }
        }
      }
    }
  },
  validationLevel: "strict",
  validationAction: "error"
});

// The three ways the app reads comments, each newest first
db.comments.createIndex({ postId: 1, createdAt: -1 }, { name: "comments_of_a_post" });
db.comments.createIndex({ "user.id": 1, createdAt: -1 }, { name: "comments_by_a_user" });
db.comments.createIndex({ "post.authorId": 1, createdAt: -1 }, { name: "comments_on_an_authors_posts" });

// ---------- reviews: one document per review ----------
db.reviews.drop();
db.createCollection("reviews", {
  validator: {
    $jsonSchema: {
      bsonType: "object",
      required: ["_id", "postId", "user", "rating", "createdAt"],
      properties: {
        _id:    { bsonType: "int", minimum: 1 },
        postId: { bsonType: "int", minimum: 1 },
        user: {
          bsonType: "object",
          required: ["id", "username"],
          properties: { id: { bsonType: "int" }, username: { bsonType: "string", minLength: 3, maxLength: 50 } }
        },
        rating:    { bsonType: "int", minimum: 1, maximum: 5 },
        text:      { bsonType: "string", minLength: 3, maxLength: 5000, description: "optional, like review_text" },
        createdAt: { bsonType: "date" }
      }
    }
  }
});
// Same rule as uq_reviews_post_user in MySQL: one review per user and post
db.reviews.createIndex({ postId: 1, "user.id": 1 }, { unique: true, name: "one_review_per_user_and_post" });

// ---------- counters: the next id of each collection (like AUTO_INCREMENT) ----------
db.counters.drop();
db.counters.insertMany([
  { _id: "comments", seq: NumberInt(3) },
  { _id: "reviews", seq: NumberInt(2) }
]);

// ---------- sample documents (real posts and users of the MySQL database) ----------
db.comments.insertMany([
  {
    _id: NumberInt(1), postId: NumberInt(20),
    post: { title: "Getting Started with JDBC", authorId: NumberInt(25) },
    user: { id: NumberInt(27), username: "dave" },
    content: "Clear introduction, thanks! The ResultSet part finally made sense.",
    createdAt: ISODate("2026-10-29T09:12:00Z"),
    replies: [
      { user: { id: NumberInt(25), username: "bob" }, content: "Glad it helped, Dave!", createdAt: ISODate("2026-10-29T10:03:00Z") },
      { user: { id: NumberInt(28), username: "erin" }, content: "Same for me.", createdAt: ISODate("2026-10-29T11:40:00Z") }
    ]
  },
  {
    _id: NumberInt(2), postId: NumberInt(20),
    post: { title: "Getting Started with JDBC", authorId: NumberInt(25) },
    user: { id: NumberInt(28), username: "erin" },
    content: "Could you add an example with generated keys?",
    createdAt: ISODate("2026-10-29T12:30:00Z"),
    editedAt: ISODate("2026-10-29T12:34:00Z")
  },
  {
    _id: NumberInt(3), postId: NumberInt(22),
    post: { title: "Indexing Strategies in MySQL 8", authorId: NumberInt(26) },
    user: { id: NumberInt(27), username: "dave" },
    content: "The leftmost-prefix rule explains a slow query I had at work.",
    createdAt: ISODate("2026-10-30T08:05:00Z")
  }
]);

db.reviews.insertMany([
  {
    _id: NumberInt(1), postId: NumberInt(20), user: { id: NumberInt(27), username: "dave" },
    rating: NumberInt(5), text: "Short and practical.", createdAt: ISODate("2026-10-29T09:15:00Z")
  },
  {
    _id: NumberInt(2), postId: NumberInt(20), user: { id: NumberInt(28), username: "erin" },
    rating: NumberInt(4), createdAt: ISODate("2026-10-29T12:31:00Z")   // no text: the field is simply left out
  }
]);

// ---------- example queries ----------
print("\n1) Comments of post 20, newest first (one read, no join):");
db.comments.find({ postId: 20 }, { _id: 1, "user.username": 1, content: 1, replies: 1 })
  .sort({ createdAt: -1 })
  .forEach(doc => printjson(doc));

print("\n2) The index is used (IXSCAN, not a full collection scan COLLSCAN):");
const plan = db.comments.find({ postId: 20 }).sort({ createdAt: -1 }).explain("executionStats");
printjson({
  stage: plan.queryPlanner.winningPlan.stage === "FETCH"
    ? "FETCH <- " + plan.queryPlanner.winningPlan.inputStage.stage + " (" + plan.queryPlanner.winningPlan.inputStage.indexName + ")"
    : plan.queryPlanner.winningPlan.stage,
  documentsExamined: plan.executionStats.totalDocsExamined,
  documentsReturned: plan.executionStats.nReturned
});

print("\n3) Average rating and number of reviews per post (aggregation, like GROUP BY):");
db.reviews.aggregate([
  { $group: { _id: "$postId", reviews: { $sum: 1 }, average: { $avg: "$rating" } } },
  { $sort: { average: -1 } }
]).forEach(doc => printjson(doc));

print("\n4) Comments that have replies (a nested array: no extra table needed):");
db.comments.find({ "replies.0": { $exists: true } }, { _id: 1, content: 1, replyCount: { $size: "$replies" } })
  .forEach(doc => printjson(doc));

print("\n5) The validator rejects a review with rating 9:");
try {
  db.reviews.insertOne({ _id: NumberInt(99), postId: NumberInt(20), user: { id: NumberInt(29), username: "frank" },
                         rating: NumberInt(9), createdAt: new Date() });
  print("   inserted (unexpected)");
} catch (e) {
  print("   rejected: " + e.message.split("\n")[0]);
}

print("\n6) The unique index rejects a second review by dave on post 20:");
try {
  db.reviews.insertOne({ _id: NumberInt(98), postId: NumberInt(20), user: { id: NumberInt(27), username: "dave" },
                         rating: NumberInt(3), createdAt: new Date() });
  print("   inserted (unexpected)");
} catch (e) {
  print("   rejected: " + e.message.split("\n")[0]);
}

print("\nDone: collections " + db.getCollectionNames().join(", "));

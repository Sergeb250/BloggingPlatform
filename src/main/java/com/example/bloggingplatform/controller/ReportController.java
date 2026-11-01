package com.example.bloggingplatform.controller;

import com.example.bloggingplatform.service.AppContext;
import com.example.bloggingplatform.model.CacheStats;
import com.example.bloggingplatform.model.Post;
import com.example.bloggingplatform.model.Role;
import com.example.bloggingplatform.model.SiteSummary;
import com.example.bloggingplatform.model.UserActivity;
import com.example.bloggingplatform.util.Icons;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Admin dashboard (analytics): totals per role and status, how the post cache is doing, the
 * best-rated and most-commented posts (click to read them), posts per tag, and a per-user activity
 * table from which an admin can open that user's posts or delete the account. The search bar
 * filters the activity table by name or role.
 */
public class ReportController extends BaseController {

    private static final int TOP = 5;
    private static final int TAG_ROWS = 8;

    @FXML private HBox statCards;
    @FXML private VBox topRated;
    @FXML private VBox mostCommented;
    @FXML private VBox postsPerTag;
    @FXML private Label activityHint;
    @FXML private TableView<UserActivity> activityTable;
    @FXML private TableColumn<UserActivity, String> userColumn;
    @FXML private TableColumn<UserActivity, Role> roleColumn;
    @FXML private TableColumn<UserActivity, String> joinedColumn;
    @FXML private TableColumn<UserActivity, Long> postsColumn;
    @FXML private TableColumn<UserActivity, Long> publishedColumn;
    @FXML private TableColumn<UserActivity, Long> draftsColumn;
    @FXML private TableColumn<UserActivity, Long> commentsColumn;
    @FXML private TableColumn<UserActivity, Long> reviewsColumn;
    @FXML private TableColumn<UserActivity, String> ratingColumn;
    @FXML private TableColumn<UserActivity, UserActivity> actionsColumn;

    private List<UserActivity> activity = List.of();

    public ReportController(AppContext app) {
        super(app);
    }

    @FXML
    private void initialize() {
        column(userColumn, UserActivity::getUsername);
        column(roleColumn, UserActivity::getRole);
        column(joinedColumn, row -> BlogWidgets.date(row.getJoined()));
        column(postsColumn, UserActivity::getPosts);
        column(publishedColumn, UserActivity::getPublished);
        column(draftsColumn, UserActivity::getDrafts);
        column(commentsColumn, UserActivity::getComments);
        column(reviewsColumn, UserActivity::getReviews);
        column(ratingColumn, row -> row.getRatingReceived() == null ? "-" : "★ %.2f".formatted(row.getRatingReceived()));
        RowActions.install(activityTable, actionsColumn, null,
                RowActions.view("View this user's posts", (UserActivity row) -> navigator.showPostsOf(row.getUserId()))
                        .onlyWhen(row -> row.getPosts() > 0),
                RowActions.delete(this::deleteUser).onlyWhen(row -> row.getUserId() != me().getId()));
    }

    @Override
    public void refresh() {
        load(() -> {
            SiteSummary summary = app.reports().summary();
            CacheStats cache = app.reports().cacheStats();
            statCards.getChildren().setAll(
                    statCard(Icons.USERS, "Users", String.valueOf(summary.getUsers()),
                            "%d admins · %d authors · %d readers".formatted(summary.getAdmins(), summary.getAuthors(), summary.getReaders())),
                    statCard(Icons.POSTS, "Posts", String.valueOf(summary.getPosts()),
                            "%d published · %d drafts".formatted(summary.getPublished(), summary.getDrafts())),
                    statCard(Icons.COMMENTS, "Comments", String.valueOf(summary.getComments()),
                            summary.getPosts() == 0 ? "" : "%.1f per post".formatted((double) summary.getComments() / summary.getPosts())),
                    statCard(Icons.REVIEWS, "Reviews", String.valueOf(summary.getReviews()), summary.getAverageRating() == null ? "no ratings yet"
                            : "average %s %.2f".formatted(BlogWidgets.stars(summary.getAverageRating()), summary.getAverageRating())),
                    statCard(Icons.DISCOVER, "Post cache", "%.0f%% hits".formatted(cache.getHitRate()),
                            "hits %d · misses %d · %d posts in memory · last load %d ms".formatted(cache.getHits(),
                                    cache.getMisses(), cache.getCachedPosts(), cache.getLastLoadMillis())));
            topRated.getChildren().setAll(ranking(app.reports().topRated(TOP),
                    post -> "★ %.1f".formatted(post.getAverageRating())));
            mostCommented.getChildren().setAll(ranking(app.reports().mostCommented(TOP),
                    post -> post.getCommentCount() + (post.getCommentCount() == 1 ? " comment" : " comments")));
            postsPerTag.getChildren().setAll(tagBars(app.reports().postsPerTag()));
            activity = app.reports().userActivity();
        });
        applyQuery();
    }

    @Override
    public String searchPrompt() {
        return "Filter the activity table by user or role";
    }

    @Override
    public void openNewForm() {
        // nothing to create on the dashboard
    }

    @Override
    protected void applyQuery() {
        List<UserActivity> shown = activity.stream()
                .filter(row -> matches(query(), row.getUsername(), row.getRole()))
                .toList();
        activityTable.getItems().setAll(shown);
        activityHint.setText(countText(shown.size(), activity.size(), "user", "users")
                + " · most posts first. Use the eye to open a user's posts; manage accounts on the Users screen.");
    }

    @Override
    protected Node root() {
        return activityTable;
    }

    private void deleteUser(UserActivity row) {
        if (confirmDelete("user " + row.getUsername(), "Their %d posts, %d comments and %d reviews are deleted too."
                .formatted(row.getPosts(), row.getComments(), row.getReviews()))
                && perform("User deleted.", () -> app.users().delete(row.getUserId()))) {
            refresh();
        }
    }

    /** One bar per tag (the most used tags first), as long as its share of the biggest count. */
    private static List<Node> tagBars(Map<String, Long> counts) {
        if (counts.isEmpty()) {
            Label empty = new Label("No tags yet.");
            empty.getStyleClass().add("empty-state");
            return List.of(empty);
        }
        long most = Math.max(1, counts.values().iterator().next());   // the first entry has the most posts
        List<Node> rows = new ArrayList<>();
        for (Map.Entry<String, Long> tag : counts.entrySet()) {
            if (rows.size() == TAG_ROWS) {
                break;
            }
            Label name = new Label("#" + tag.getKey());
            name.getStyleClass().add("tag-bar-name");
            Region bar = new Region();
            bar.getStyleClass().add("tag-bar");
            bar.setPrefWidth(Math.max(4, 100.0 * tag.getValue() / most));
            bar.setMinWidth(Region.USE_PREF_SIZE);   // never squeezed: the length is the information
            Label count = new Label(String.valueOf(tag.getValue()));
            count.getStyleClass().add("tag-bar-count");
            count.setMinWidth(Region.USE_PREF_SIZE);
            HBox row = new HBox(8, name, bar, count);
            row.setAlignment(Pos.CENTER_LEFT);
            row.getStyleClass().add("rank-row");
            rows.add(row);
        }
        return rows;
    }

    private static Node statCard(String icon, String title, String value, String detail) {
        Label name = new Label(title, Icons.of(icon, 18));
        name.getStyleClass().add("stat-title");
        Label number = new Label(value);
        number.getStyleClass().add("stat-value");
        Label more = new Label(detail);
        more.getStyleClass().add("stat-detail");
        more.setWrapText(true);
        VBox card = new VBox(4, name, number, more);
        card.getStyleClass().add("stat-card");
        card.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(card, Priority.ALWAYS);
        return card;
    }

    /** Numbered rows "1. Title — value"; a click on the title opens the post. */
    private List<Node> ranking(List<Post> posts, Function<Post, String> value) {
        if (posts.isEmpty()) {
            Label empty = new Label("Nothing to rank yet.");
            empty.getStyleClass().add("empty-state");
            return List.of(empty);
        }
        return posts.stream().map(post -> {
            Label rank = new Label((posts.indexOf(post) + 1) + ".");
            rank.getStyleClass().add("rank-number");
            Button title = new Button(post.getTitle());
            title.getStyleClass().add("btn-link");
            title.setOnAction(e -> navigator.openPost(post.getId()));
            title.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(title, Priority.ALWAYS);
            Label author = new Label("by " + post.getAuthorName());
            author.getStyleClass().add("card-meta");
            author.setMinWidth(Region.USE_PREF_SIZE);
            Label score = new Label(value.apply(post));
            score.getStyleClass().add("rank-value");
            score.setMinWidth(Region.USE_PREF_SIZE);
            HBox row = new HBox(8, rank, title, author, score);
            row.setAlignment(Pos.CENTER_LEFT);
            row.getStyleClass().add("rank-row");
            return (Node) row;
        }).toList();
    }
}

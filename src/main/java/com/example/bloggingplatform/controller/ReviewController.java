package com.example.bloggingplatform.controller;

import com.example.bloggingplatform.service.AppContext;
import com.example.bloggingplatform.model.Post;
import com.example.bloggingplatform.model.Review;
import com.example.bloggingplatform.model.User;
import com.example.bloggingplatform.validations.ReviewValidator;
import com.example.bloggingplatform.service.ReviewService;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Review table for three audiences: every review with full CRUD ({@link Scope#ALL}, admins),
 * reviews of the author's posts, read-only so ratings stay honest ({@link Scope#ON_MY_POSTS}),
 * and the user's own reviews ({@link Scope#MINE}). The subtitle shows the average rating of the
 * rows on screen.
 */
public class ReviewController extends BaseController {

    @FXML private Label screenTitle;
    @FXML private Label countLabel;
    @FXML private Button newButton;
    @FXML private ComboBox<FilterOption<Post>> postFilter;
    @FXML private TableView<Review> table;
    @FXML private TableColumn<Review, Integer> idColumn;
    @FXML private TableColumn<Review, String> postColumn;
    @FXML private TableColumn<Review, String> userColumn;
    @FXML private TableColumn<Review, String> ratingColumn;
    @FXML private TableColumn<Review, String> textColumn;
    @FXML private TableColumn<Review, String> createdColumn;
    @FXML private TableColumn<Review, Review> actionsColumn;

    @FXML private VBox form;
    @FXML private Label formMessage;
    @FXML private ComboBox<Post> postBox;
    @FXML private VBox userCell;
    @FXML private ComboBox<User> userBox;
    @FXML private ComboBox<Integer> ratingBox;
    @FXML private TextArea textArea;
    @FXML private Label postError;
    @FXML private Label userError;
    @FXML private Label ratingError;
    @FXML private Label textError;

    private List<Review> loaded = List.of();
    private List<Post> visiblePosts = List.of();
    private boolean filling;

    public ReviewController(AppContext app) {
        super(app);
    }

    @FXML
    private void initialize() {
        column(idColumn, Review::getId);
        column(postColumn, Review::getPostTitle);
        column(userColumn, Review::getUsername);
        column(ratingColumn, review -> BlogWidgets.stars(review.getRating()));
        column(textColumn, review -> review.getReviewText() == null ? "" : review.getReviewText());
        column(createdColumn, review -> format(review.getCreatedAt()));
        RowActions.install(table, actionsColumn, review -> navigator.openPost(review.getPostId()),
                RowActions.view("Open the post", review -> navigator.openPost(review.getPostId())),
                RowActions.edit(this::edit).onlyWhen(review -> app.reviews().canManage(review)),
                RowActions.delete(this::delete).onlyWhen(review -> app.reviews().canManage(review)));
        newButton(newButton);
        ratingBox.getItems().setAll(5, 4, 3, 2, 1);
        ratingBox.setButtonCell(new StarCell());
        ratingBox.setCellFactory(list -> new StarCell());
        errors.bindSummary(formMessage);
        errors.bind("post", postBox, postError);
        errors.bind("user", userBox, userError);
        errors.bind("rating", ratingBox, ratingError);
        errors.bind("text", textArea, textError);
        errors.live("text", textArea, () -> ReviewValidator.reviewText(textArea.getText()));
    }

    @Override
    protected void scopeChanged() {
        screenTitle.setText(switch (scope) {
            case ALL -> "Reviews";
            case MINE -> "My reviews";
            case ON_MY_POSTS -> "Reviews of my posts";
        });
        boolean canCreate = scope != Scope.ON_MY_POSTS;
        newButton.setVisible(canCreate);
        newButton.setManaged(canCreate);
        userCell.setVisible(scope == Scope.ALL);
        userCell.setManaged(scope == Scope.ALL);
        userColumn.setVisible(scope != Scope.MINE);
    }

    @Override
    public void refresh() {
        filling = true;
        load(() -> {
            loaded = switch (scope) {
                case ALL -> app.reviews().find(null);
                case MINE -> app.reviews().findMine();
                case ON_MY_POSTS -> app.reviews().findOnMyPosts();
            };
            visiblePosts = app.posts().findAll();
            Set<Integer> reviewed = loaded.stream().map(Review::getPostId).collect(Collectors.toSet());
            List<Post> filterPosts = scope == Scope.ALL ? visiblePosts
                    : visiblePosts.stream().filter(post -> reviewed.contains(post.getId())).toList();
            FilterOption.fill(postFilter, "All posts", filterPosts, Post::getId);
            // reviews are only allowed on published posts, and never on your own
            int me = me().getId();
            postBox.getItems().setAll(app.posts().findPublished().stream()
                    .filter(post -> scope == Scope.ALL || post.getAuthorId() != me).toList());
            if (scope == Scope.ALL) {
                userBox.getItems().setAll(app.users().findAll());
            }
        });
        filling = false;
        applyQuery();
    }

    @Override
    public String searchPrompt() {
        return "Search reviews by text, post or reviewer";
    }

    @Override
    protected void applyQuery() {
        Post post = FilterOption.selected(postFilter);
        List<Review> shown = loaded.stream()
                .filter(review -> post == null || review.getPostId() == post.getId())
                .filter(review -> matches(query(), review.getReviewText(), review.getPostTitle(), review.getUsername()))
                .toList();
        table.getItems().setAll(shown);
        Double average = ReviewService.average(shown);
        countLabel.setText(countText(shown.size(), loaded.size(), "review", "reviews")
                + (average == null ? "" : " · average %.2f / 5".formatted(average)));
    }

    @Override
    protected Node root() {
        return table;
    }

    @Override
    public void openNewForm() {
        if (scope == Scope.ON_MY_POSTS) {
            return;
        }
        errors.clear();
        postBox.setDisable(false);
        userBox.setDisable(false);
        postBox.setValue(null);
        userBox.setValue(null);
        ratingBox.setValue(null);
        textArea.clear();
        openForm("New review", form, true, postBox, () -> {
            Post post = postBox.getValue();
            User user = userBox.getValue();
            boolean saved = save("Review added.", () -> app.reviews().create(post == null ? null : post.getId(),
                    user == null ? null : user.getId(), ratingBox.getValue(), textArea.getText())).isPresent();
            if (saved) {
                refresh();
            }
            return saved;
        });
    }

    @FXML
    private void onNew() {
        openNewForm();
    }

    @FXML
    private void onFilterChanged() {
        if (!filling) {
            applyQuery();
        }
    }

    /** Rating and text can change; the post and reviewer are shown but locked. */
    private void edit(Review review) {
        if (!app.reviews().canManage(review)) {
            return;
        }
        errors.clear();
        postBox.setValue(findFirst(visiblePosts, post -> post.getId() == review.getPostId()));
        userBox.setValue(findFirst(userBox.getItems(), user -> Objects.equals(user.getId(), review.getUserId())));
        postBox.setDisable(true);
        userBox.setDisable(true);
        ratingBox.setValue(review.getRating());
        textArea.setText(review.getReviewText() == null ? "" : review.getReviewText());
        openForm("Edit review #" + review.getId(), form, false, ratingBox, () -> {
            boolean saved = save("Review updated.",
                    () -> app.reviews().update(review.getId(), ratingBox.getValue(), textArea.getText())).isPresent();
            if (saved) {
                refresh();
            }
            return saved;
        });
    }

    private void delete(Review review) {
        if (confirmDelete("the review by " + review.getUsername(), "The rating no longer counts towards the post's average.")
                && perform("Review deleted.", () -> app.reviews().delete(review.getId()))) {
            refresh();
        }
    }

    /** Shows a rating as "★★★★☆ 4". */
    private static final class StarCell extends javafx.scene.control.ListCell<Integer> {
        @Override
        protected void updateItem(Integer rating, boolean empty) {
            super.updateItem(rating, empty);
            setText(empty || rating == null ? null : BlogWidgets.stars(rating) + "  " + rating);
        }
    }
}

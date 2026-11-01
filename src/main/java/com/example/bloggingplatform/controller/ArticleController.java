package com.example.bloggingplatform.controller;

import com.example.bloggingplatform.service.AppContext;
import com.example.bloggingplatform.model.Comment;
import com.example.bloggingplatform.model.Post;
import com.example.bloggingplatform.model.PostStatus;
import com.example.bloggingplatform.model.Review;
import com.example.bloggingplatform.model.User;
import com.example.bloggingplatform.validations.ReviewValidator;
import com.example.bloggingplatform.service.ReviewService;
import com.example.bloggingplatform.util.Icons;
import com.example.bloggingplatform.util.PopupWindow;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Reading view of one post. The middle shows only the article; the right panel shows the
 * ratings (average, 5-to-1 distribution and a row of stars: one click opens the rating pop-up
 * with that many stars chosen) and, under it, the comments or the reviews. Writing a comment
 * also happens in a pop-up. Edit and delete buttons appear only where the role allows.
 */
public class ArticleController extends BaseController {

    private static final String[] RATING_WORDS = {"", "Poor", "Fair", "Good", "Very good", "Excellent"};

    @FXML private ScrollPane scroll;
    @FXML private Button backButton;
    @FXML private Label statusBadge;
    @FXML private FlowPane tagBox;
    @FXML private Label titleLabel;
    @FXML private HBox metaBox;
    @FXML private Label draftNote;
    @FXML private VBox body;

    @FXML private Label averageLabel;
    @FXML private Label averageStars;
    @FXML private Label reviewCount;
    @FXML private VBox distribution;
    @FXML private VBox rateBox;
    @FXML private Label rateHint;
    @FXML private HBox starPicker;
    @FXML private Label reviewNote;

    @FXML private ToggleButton commentsTab;
    @FXML private ToggleButton reviewsTab;
    @FXML private Button writeCommentButton;
    @FXML private Label commentNote;
    @FXML private VBox itemList;

    private final ToggleGroup lists = new ToggleGroup();
    private final List<Button> stars = new ArrayList<>();
    private int postId;
    private Post post;
    private Review myReview;
    private List<Comment> comments = List.of();
    private List<Review> reviews = List.of();

    public ArticleController(AppContext app) {
        super(app);
    }

    @FXML
    private void initialize() {
        backButton.setGraphic(Icons.of(Icons.BACK, 16));
        writeCommentButton.setGraphic(Icons.of(Icons.COMMENTS, 15));
        for (int i = 1; i <= 5; i++) {
            int rating = i;
            Button star = new Button("☆");
            star.getStyleClass().add("star-button");
            star.setTooltip(new Tooltip(rating + (rating == 1 ? " star" : " stars") + " · " + RATING_WORDS[rating]));
            star.setOnAction(e -> openRating(rating));          // one click opens the rating pop-up
            star.setOnMouseEntered(e -> BlogWidgets.paintStars(stars, rating));
            star.setOnMouseExited(e -> BlogWidgets.paintStars(stars, myRating()));
            stars.add(star);
        }
        starPicker.getChildren().setAll(stars);

        commentsTab.setToggleGroup(lists);
        reviewsTab.setToggleGroup(lists);
        lists.selectToggle(commentsTab);
        lists.selectedToggleProperty().addListener((obs, old, now) -> {
            if (now == null) {
                lists.selectToggle(old);   // one list is always shown
            } else if (post != null) {
                showList();
            }
        });
    }

    /** The post to show; it is loaded by {@link #refresh()}. */
    public void setPost(int postId) {
        if (postId != this.postId) {
            scroll.setVvalue(0);
            lists.selectToggle(commentsTab);
        }
        this.postId = postId;
    }

    @Override
    public void refresh() {
        load(() -> {
            Post found = app.posts().findById(postId);
            if (found == null) {
                Dialogs.info(window(), "Post not available", "This post was deleted or is no longer published.");
                navigator.back();
                return;
            }
            post = found;
            reviews = app.reviews().find(postId);
            comments = app.comments().find(postId);
            showArticle();
            showRatings();
            showList();
        });
    }

    @Override
    public String searchPrompt() {
        return "Search posts by title or content";
    }

    @Override
    public void openNewForm() {
        if (writeCommentButton.isVisible()) {
            onWriteComment();
        }
    }

    @Override
    protected void applyQuery() {
        // searching from the reading view returns to Discover (see ShellController)
    }

    @Override
    protected Node root() {
        return body;
    }

    @FXML
    private void onBack() {
        navigator.back();
    }

    /** "Write a comment" pop-up. */
    @FXML
    private void onWriteComment() {
        CommentForm form = new CommentForm("");
        openForm("Comment on \"" + post.getTitle() + "\"", form.root, "Post comment", true, form.text, () -> {
            boolean saved = save(form.errors, "Comment posted.",
                    () -> app.comments().create(postId, null, form.text.getText())).isPresent();
            if (saved) {
                lists.selectToggle(commentsTab);
                refresh();
            }
            return saved;
        });
    }

    // ---------- the article (middle) ----------

    private void showArticle() {
        boolean draft = post.getStatus() == PostStatus.DRAFT;
        BlogWidgets.show(statusBadge, draft);
        tagBox.getChildren().setAll(post.getTags().stream().map(tag -> BlogWidgets.tagChip(tag, null)).toList());
        titleLabel.setText(post.getTitle());

        Label author = new Label(post.getAuthorName());
        author.getStyleClass().add("card-author");
        String edited = post.getUpdatedAt() != null && post.getCreatedAt() != null
                && post.getUpdatedAt().isAfter(post.getCreatedAt().plusMinutes(1))
                ? " · updated " + BlogWidgets.date(post.getUpdatedAt()) : "";
        Label when = new Label(BlogWidgets.date(post.getCreatedAt()) + edited + " · " + BlogWidgets.readingTime(post.getContent()));
        when.getStyleClass().add("card-meta");
        metaBox.getChildren().setAll(BlogWidgets.avatar(post.getAuthorName(), 40), new VBox(2, author, when));

        BlogWidgets.show(draftNote, draft);
        draftNote.setText(draft ? "This post is a draft: only its author and admins can see it. "
                + "Comments and ratings open once it is published." : null);

        // blank lines separate paragraphs
        body.getChildren().setAll(Arrays.stream(post.getContent().strip().split("\\R\\s*\\R"))
                .map(paragraph -> {
                    Label text = new Label(paragraph.strip());
                    text.setWrapText(true);
                    text.setMinHeight(Region.USE_PREF_SIZE);
                    text.getStyleClass().add("article-paragraph");
                    return (Node) text;
                }).toList());
    }

    // ---------- ratings (right panel) ----------

    private void showRatings() {
        Double average = ReviewService.average(reviews);
        averageLabel.setText(average == null ? "–" : "%.1f".formatted(average));
        averageStars.setText(BlogWidgets.stars(average));
        reviewCount.setText(reviews.size() + (reviews.size() == 1 ? " review" : " reviews"));
        distribution.getChildren().clear();
        for (int rating = 5; rating >= 1; rating--) {
            int stars = rating;
            long count = reviews.stream().filter(review -> review.getRating() == stars).count();
            Label label = new Label(stars + " ★");
            label.getStyleClass().add("bar-label");
            ProgressBar bar = new ProgressBar(reviews.isEmpty() ? 0 : (double) count / reviews.size());
            bar.getStyleClass().add("rating-bar");
            bar.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(bar, Priority.ALWAYS);
            Label number = new Label(String.valueOf(count));
            number.getStyleClass().add("bar-count");
            HBox row = new HBox(8, label, bar, number);
            row.setAlignment(Pos.CENTER_LEFT);
            distribution.getChildren().add(row);
        }

        User me = me();
        myReview = reviews.stream().filter(review -> review.getUserId() == me.getId()).findFirst().orElse(null);
        boolean published = post.getStatus() == PostStatus.PUBLISHED;
        boolean ownPost = post.getAuthorId() == me.getId();
        BlogWidgets.show(rateBox, published && !ownPost);
        BlogWidgets.show(reviewNote, published && ownPost);
        reviewNote.setText("Authors cannot rate their own posts.");
        rateHint.setText(myReview == null ? "Click a star to rate this post" : "Your rating · click to change it");
        BlogWidgets.paintStars(stars, myRating());
    }

    private int myRating() {
        return myReview == null ? 0 : myReview.getRating();
    }

    /** The rating pop-up: stars, optional text, and Submit / Update (and Delete for an existing review). */
    private void openRating(int preset) {
        FormErrors rateErrors = new FormErrors();
        int[] chosen = {preset};
        Label banner = BlogWidgets.formMessage();
        Label postTitle = new Label(post.getTitle());
        postTitle.getStyleClass().add("compose-title");
        postTitle.setWrapText(true);

        List<Button> bigStars = new ArrayList<>();
        Label ratingText = new Label();
        ratingText.getStyleClass().add("rating-words");
        for (int i = 1; i <= 5; i++) {
            int rating = i;
            Button star = new Button("☆");
            star.getStyleClass().addAll("star-button", "star-big");
            star.setOnAction(e -> {
                chosen[0] = rating;
                BlogWidgets.paintStars(bigStars, rating);
                ratingText.setText(rating + " of 5 · " + RATING_WORDS[rating]);
            });
            star.setOnMouseEntered(e -> BlogWidgets.paintStars(bigStars, rating));
            star.setOnMouseExited(e -> BlogWidgets.paintStars(bigStars, chosen[0]));
            bigStars.add(star);
        }
        HBox starRow = new HBox(2);
        starRow.getChildren().setAll(bigStars);
        starRow.getStyleClass().add("star-picker");
        BlogWidgets.paintStars(bigStars, preset);
        ratingText.setText(preset + " of 5 · " + RATING_WORDS[preset]);
        HBox ratingLine = new HBox(12, starRow, ratingText);
        ratingLine.setAlignment(Pos.CENTER_LEFT);
        Label ratingError = BlogWidgets.fieldError();

        TextArea text = new TextArea(myReview == null || myReview.getReviewText() == null ? "" : myReview.getReviewText());
        text.setPromptText("What did you think? (optional)");
        text.setWrapText(true);
        text.setPrefRowCount(5);
        Label textError = BlogWidgets.fieldError();
        VBox form = new VBox(banner, postTitle, BlogWidgets.fieldLabel("Your rating"), ratingLine, ratingError,
                BlogWidgets.fieldLabel("Review (optional, 3-5000 characters)"), text, textError);
        form.getStyleClass().add("form");
        form.setPrefWidth(480);
        rateErrors.bindSummary(banner);
        rateErrors.bind("rating", starRow, ratingError);
        rateErrors.bind("text", text, textError);
        rateErrors.live("text", text, () -> ReviewValidator.reviewText(text.getText()));

        PopupWindow popup = new PopupWindow(window(), myReview == null ? "Rate this post" : "Your review");
        popup.setContent(form);
        popup.addButton("Cancel", null, popup::close);
        if (myReview != null) {
            int id = myReview.getId();
            popup.addButton("Delete review", "btn-delete", () -> {
                if (confirmDelete("your review", "Your rating no longer counts towards this post's average.")
                        && perform("Your review was deleted.", () -> app.reviews().delete(id))) {
                    popup.close();
                    refresh();
                }
            });
        }
        Button confirm = popup.addButton(myReview == null ? "Submit rating" : "Update rating",
                myReview == null ? "btn-create" : "btn-update", () -> {
                    Integer rating = chosen[0];
                    boolean saved = myReview == null
                            ? save(rateErrors, "Thanks for rating this post.",
                            () -> app.reviews().create(postId, null, rating, text.getText())).isPresent()
                            : save(rateErrors, "Your review was updated.",
                            () -> app.reviews().update(myReview.getId(), rating, text.getText())).isPresent();
                    if (saved) {
                        popup.close();
                        lists.selectToggle(reviewsTab);
                        refresh();
                    }
                });
        confirm.setGraphic(Icons.of(Icons.REVIEWS, 14));
        confirm.setDefaultButton(true);
        popup.setOnClosed(() -> BlogWidgets.paintStars(stars, myRating()));
        popup.showAndWait(text);
    }

    // ---------- comments / reviews (right panel) ----------

    private void showList() {
        commentsTab.setText("Comments (" + comments.size() + ")");
        reviewsTab.setText("Reviews (" + reviews.size() + ")");
        boolean showComments = lists.getSelectedToggle() == commentsTab;
        boolean published = post.getStatus() == PostStatus.PUBLISHED;
        BlogWidgets.show(writeCommentButton, showComments && published);
        BlogWidgets.show(commentNote, showComments && !published);
        commentNote.setText("Comments open once the post is published.");
        List<Node> items = showComments
                ? comments.stream().map(this::commentItem).toList()
                : reviews.stream().map(this::reviewItem).toList();
        if (items.isEmpty()) {
            Label empty = new Label(showComments ? "No comments yet. Start the conversation!" : "No reviews yet.");
            empty.getStyleClass().add("empty-state");
            empty.setWrapText(true);
            items = List.of(empty);
        }
        itemList.getChildren().setAll(items);
    }

    private Node reviewItem(Review review) {
        Label stars = new Label(BlogWidgets.stars(review.getRating()));
        stars.getStyleClass().add("stars-small");
        HBox head = BlogWidgets.itemHead(review.getUsername(), review.getCreatedAt(), review.getUserId() == me().getId() ? BlogWidgets.youBadge() : null);
        if (review.getUserId() == me().getId()) {
            head.getChildren().add(BlogWidgets.iconButton(Icons.EDIT, "btn-update", "Change your review",
                    () -> openRating(review.getRating())));
        }
        if (app.reviews().canManage(review)) {
            head.getChildren().add(BlogWidgets.iconButton(Icons.DELETE, "btn-delete", "Delete review", () -> {
                if (confirmDelete("the review by " + review.getUsername(), "The rating no longer counts towards the average.")
                        && perform("Review deleted.", () -> app.reviews().delete(review.getId()))) {
                    refresh();
                }
            }));
        }
        VBox item = new VBox(4, head, stars);
        if (review.getReviewText() != null) {
            item.getChildren().add(BlogWidgets.itemText(review.getReviewText()));
        }
        item.getStyleClass().add("item-card");
        return item;
    }

    private Node commentItem(Comment comment) {
        Node badge = comment.getUserId() == post.getAuthorId() ? BlogWidgets.badge("Author")
                : comment.getUserId() == me().getId() ? BlogWidgets.youBadge() : null;
        HBox head = BlogWidgets.itemHead(comment.getUsername(), comment.getCreatedAt(), badge);
        if (app.comments().canEdit(comment)) {
            head.getChildren().add(BlogWidgets.iconButton(Icons.EDIT, "btn-update", "Edit comment", () -> editComment(comment)));
        }
        if (app.comments().canDelete(comment)) {
            head.getChildren().add(BlogWidgets.iconButton(Icons.DELETE, "btn-delete", "Delete comment", () -> {
                if (confirmDelete("this comment", "It is removed from the post.")
                        && perform("Comment deleted.", () -> app.comments().delete(comment.getId()))) {
                    refresh();
                }
            }));
        }
        VBox item = new VBox(4, head, BlogWidgets.itemText(comment.getContent()));
        item.getStyleClass().add("item-card");
        return item;
    }

    /** Edits a comment's text in a pop-up. */
    private void editComment(Comment comment) {
        CommentForm form = new CommentForm(comment.getContent());
        openForm("Edit comment", form.root, false, form.text, () -> {
            boolean saved = save(form.errors, "Comment updated.",
                    () -> app.comments().update(comment.getId(), form.text.getText())).isPresent();
            if (saved) {
                refresh();
            }
            return saved;
        });
    }

}

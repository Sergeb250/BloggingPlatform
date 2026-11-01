package com.example.bloggingplatform.controller;

import com.example.bloggingplatform.service.AppContext;
import com.example.bloggingplatform.model.Comment;
import com.example.bloggingplatform.model.Post;
import com.example.bloggingplatform.model.User;
import com.example.bloggingplatform.validations.CommentValidator;
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
 * Comment table for three audiences: every comment with full CRUD ({@link Scope#ALL}, admins),
 * comments on the author's posts, which the author may delete ({@link Scope#ON_MY_POSTS}), and
 * the user's own comments ({@link Scope#MINE}). Rows are filtered by post and by the search bar
 * in memory.
 */
public class CommentController extends BaseController {

    @FXML private Label screenTitle;
    @FXML private Label countLabel;
    @FXML private Button newButton;
    @FXML private ComboBox<FilterOption<Post>> postFilter;
    @FXML private TableView<Comment> table;
    @FXML private TableColumn<Comment, Integer> idColumn;
    @FXML private TableColumn<Comment, String> postColumn;
    @FXML private TableColumn<Comment, String> userColumn;
    @FXML private TableColumn<Comment, String> contentColumn;
    @FXML private TableColumn<Comment, String> createdColumn;
    @FXML private TableColumn<Comment, Comment> actionsColumn;

    @FXML private VBox form;
    @FXML private Label formMessage;
    @FXML private ComboBox<Post> postBox;
    @FXML private VBox userCell;
    @FXML private ComboBox<User> userBox;
    @FXML private TextArea contentArea;
    @FXML private Label postError;
    @FXML private Label userError;
    @FXML private Label contentError;

    private List<Comment> loaded = List.of();
    private List<Post> visiblePosts = List.of();
    private boolean filling;

    public CommentController(AppContext app) {
        super(app);
    }

    @FXML
    private void initialize() {
        column(idColumn, Comment::getId);
        column(postColumn, Comment::getPostTitle);
        column(userColumn, Comment::getUsername);
        column(contentColumn, Comment::getContent);
        column(createdColumn, comment -> format(comment.getCreatedAt()));
        RowActions.install(table, actionsColumn, comment -> navigator.openPost(comment.getPostId()),
                RowActions.view("Open the post", comment -> navigator.openPost(comment.getPostId())),
                RowActions.edit(this::edit).onlyWhen(comment -> app.comments().canEdit(comment)),
                RowActions.delete(this::delete));
        newButton(newButton);
        errors.bindSummary(formMessage);
        errors.bind("post", postBox, postError);
        errors.bind("user", userBox, userError);
        errors.bind("content", contentArea, contentError);
        errors.live("content", contentArea, () -> CommentValidator.comment(contentArea.getText()));
    }

    @Override
    protected void scopeChanged() {
        screenTitle.setText(switch (scope) {
            case ALL -> "Comments";
            case MINE -> "My comments";
            case ON_MY_POSTS -> "Comments on my posts";
        });
        boolean canCreate = scope != Scope.ON_MY_POSTS;   // authors answer from the post itself
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
                case ALL -> app.comments().find(null);
                case MINE -> app.comments().findMine();
                case ON_MY_POSTS -> app.comments().findOnMyPosts();
            };
            visiblePosts = app.posts().findAll();
            Set<Integer> commented = loaded.stream().map(Comment::getPostId).collect(Collectors.toSet());
            List<Post> filterPosts = scope == Scope.ALL ? visiblePosts
                    : visiblePosts.stream().filter(post -> commented.contains(post.getId())).toList();
            FilterOption.fill(postFilter, "All posts", filterPosts, Post::getId);
            // new comments are only allowed on published posts, so only those are offered
            postBox.getItems().setAll(app.posts().findPublished());
            if (scope == Scope.ALL) {
                userBox.getItems().setAll(app.users().findAll());
            }
        });
        filling = false;
        applyQuery();
    }

    @Override
    public String searchPrompt() {
        return "Search comments by text, post or writer";
    }

    @Override
    protected void applyQuery() {
        Post post = FilterOption.selected(postFilter);
        List<Comment> shown = loaded.stream()
                .filter(comment -> post == null || comment.getPostId() == post.getId())
                .filter(comment -> matches(query(), comment.getContent(), comment.getPostTitle(), comment.getUsername()))
                .toList();
        table.getItems().setAll(shown);
        countLabel.setText(countText(shown.size(), loaded.size(), "comment", "comments"));
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
        postBox.setValue(FilterOption.selected(postFilter));
        userBox.setValue(null);
        contentArea.clear();
        openForm("New comment", form, true, contentArea, () -> {
            Post post = postBox.getValue();
            User user = userBox.getValue();
            boolean saved = save("Comment added.", () -> app.comments().create(post == null ? null : post.getId(),
                    user == null ? null : user.getId(), contentArea.getText())).isPresent();
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

    /** Only the text can change; the post and writer are shown but locked. */
    private void edit(Comment comment) {
        if (!app.comments().canEdit(comment)) {
            return;
        }
        errors.clear();
        postBox.setValue(findFirst(visiblePosts, post -> post.getId() == comment.getPostId()));
        userBox.setValue(findFirst(userBox.getItems(), user -> Objects.equals(user.getId(), comment.getUserId())));
        postBox.setDisable(true);
        userBox.setDisable(true);
        contentArea.setText(comment.getContent());
        openForm("Edit comment #" + comment.getId(), form, false, contentArea, () -> {
            boolean saved = save("Comment updated.", () -> app.comments().update(comment.getId(), contentArea.getText()))
                    .isPresent();
            if (saved) {
                refresh();
            }
            return saved;
        });
    }

    private void delete(Comment comment) {
        if (confirmDelete("the comment by " + comment.getUsername(), "It is removed from \"" + comment.getPostTitle() + "\".")
                && perform("Comment deleted.", () -> app.comments().delete(comment.getId()))) {
            refresh();
        }
    }
}

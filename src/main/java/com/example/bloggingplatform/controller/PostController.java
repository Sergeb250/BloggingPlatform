package com.example.bloggingplatform.controller;

import com.example.bloggingplatform.service.AppContext;
import com.example.bloggingplatform.model.Page;
import com.example.bloggingplatform.model.Post;
import com.example.bloggingplatform.model.PostFilter;
import com.example.bloggingplatform.model.PostStatus;
import com.example.bloggingplatform.model.Tag;
import com.example.bloggingplatform.model.User;
import com.example.bloggingplatform.validations.PostValidator;
import com.example.bloggingplatform.validations.TagValidator;
import com.example.bloggingplatform.util.Icons;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.Pagination;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Post table (US 2.1, 2.2): "My posts" for authors ({@link Scope#MINE}), every post for admins
 * ({@link Scope#ALL}). Only one page of rows is ever loaded: the {@link Pagination} control is
 * the page navigator and each page change runs a {@code LIMIT ? OFFSET ?} query. The keyword
 * from the search bar and the status / tag / author filters are applied in SQL.
 */
public class PostController extends BaseController {

    static final int PAGE_SIZE = 10;

    @FXML private Label screenTitle;
    @FXML private Label stats;
    @FXML private Button newButton;
    @FXML private ComboBox<FilterOption<PostStatus>> statusFilter;
    @FXML private ComboBox<FilterOption<Tag>> tagFilter;
    @FXML private Label authorFilterLabel;
    @FXML private ComboBox<FilterOption<User>> authorFilter;

    @FXML private TableView<Post> table;
    @FXML private TableColumn<Post, String> titleColumn;
    @FXML private TableColumn<Post, String> authorColumn;
    @FXML private TableColumn<Post, PostStatus> statusColumn;
    @FXML private TableColumn<Post, String> tagsColumn;
    @FXML private TableColumn<Post, Integer> commentsColumn;
    @FXML private TableColumn<Post, String> ratingColumn;
    @FXML private TableColumn<Post, String> updatedColumn;
    @FXML private TableColumn<Post, Post> actionsColumn;
    @FXML private Pagination pagination;
    @FXML private Label pageInfo;

    @FXML private VBox form;
    @FXML private Label formMessage;
    @FXML private VBox authorCell;
    @FXML private ComboBox<User> authorBox;
    @FXML private TextField titleField;
    @FXML private ComboBox<PostStatus> statusBox;
    @FXML private FlowPane tagInput;
    @FXML private TextField newTagField;
    @FXML private HBox suggestionRow;
    @FXML private Label suggestionLabel;
    @FXML private FlowPane tagSuggestions;
    @FXML private TextArea contentArea;
    @FXML private Label authorError;
    @FXML private Label titleError;
    @FXML private Label statusError;
    @FXML private Label tagsError;
    @FXML private Label contentError;

    /** Tag names chosen in the form; tags that do not exist yet are created when the post is saved. */
    private final ObservableList<String> chosenTags = FXCollections.observableArrayList();
    /** Tags offered in the form: the author's own tags (every tag for an admin). */
    private List<Tag> writingTags = List.of();
    private PostFilter filter = PostFilter.none();
    /** Index of the page currently in the table, -1 before the first load. */
    private int shownPage = -1;
    /** True while the filter boxes are refilled, so their change events are ignored. */
    private boolean filling;

    public PostController(AppContext app) {
        super(app);
    }

    @FXML
    private void initialize() {
        column(titleColumn, Post::getTitle);
        column(authorColumn, Post::getAuthorName);
        column(statusColumn, Post::getStatus);
        statusColumn.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(PostStatus status, boolean empty) {
                super.updateItem(status, empty);
                setText(null);
                setGraphic(empty || status == null ? null : BlogWidgets.statusBadge(status));
            }
        });
        column(tagsColumn, Post::getTagNames);
        column(commentsColumn, Post::getCommentCount);
        column(ratingColumn, post -> post.getAverageRating() == null ? "-" : "★ %.1f".formatted(post.getAverageRating()));
        column(updatedColumn, post -> post.getUpdatedAt() == null ? "" : post.getUpdatedAt().toLocalDate().toString());
        RowActions.install(table, actionsColumn, this::edit,
                RowActions.view("Read the post", post -> navigator.openPost(post.getId())),
                RowActions.edit(this::edit),
                new RowActions.Action<>(post -> post.getStatus() == PostStatus.PUBLISHED ? "Move back to drafts" : "Publish",
                        Icons.PUBLISH, null, this::togglePublished, post -> true),
                RowActions.delete(this::delete));
        newButton(newButton);

        statusFilter.getItems().add(FilterOption.all("All statuses"));
        for (PostStatus status : PostStatus.values()) {
            statusFilter.getItems().add(FilterOption.of(status));
        }
        statusFilter.getSelectionModel().selectFirst();
        statusBox.getItems().setAll(PostStatus.values());
        chosenTags.addListener((ListChangeListener<String>) change -> renderTags());
        tagInput.setOnMouseClicked(e -> newTagField.requestFocus());
        newTagField.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == KeyCode.BACK_SPACE && newTagField.getText().isEmpty() && !chosenTags.isEmpty()) {
                chosenTags.removeLast();   // Backspace in the empty field removes the last chip
                e.consume();
            }
        });
        newTagField.textProperty().addListener((obs, old, text) -> {
            if (text.contains(",")) {
                Platform.runLater(this::onAddTag);   // a comma also finishes a tag
            }
        });
        pagination.setPageFactory(index -> {
            if (index != shownPage) {
                showPage(index);
            }
            return new Region();   // rows are in the table above; the control is only the page navigator
        });

        errors.bindSummary(formMessage);
        errors.bind("author", authorBox, authorError);
        errors.bind("title", titleField, titleError);
        errors.bind("status", statusBox, statusError);
        errors.bind("tags", tagInput, tagsError);
        errors.bind("content", contentArea, contentError);
        errors.live("title", titleField, () -> PostValidator.title(titleField.getText()));
        errors.live("content", contentArea, () -> PostValidator.content(contentArea.getText()));
        errors.live("tags", newTagField, this::tagProblem);
        FormErrors.limit(titleField, PostValidator.TITLE_MAX);
    }

    @Override
    protected void scopeChanged() {
        boolean mine = scope == Scope.MINE;
        screenTitle.setText(mine ? "My posts" : "All posts");
        newButton.setText(mine ? "Write a post" : "New post");
        for (Node node : List.of(authorFilterLabel, authorFilter, authorCell)) {
            node.setVisible(!mine);
            node.setManaged(!mine);
        }
        authorColumn.setVisible(!mine);
        if (mine) {
            filter = new PostFilter(null, me().getId(), null, null);
        }
    }

    @Override
    public void refresh() {
        filling = true;
        load(() -> {
            List<User> authors = app.users().findAuthors();
            FilterOption.fill(authorFilter, "All authors", authors, User::getId);
            authorBox.getItems().setAll(authors);
            reloadTags();
        });
        filling = false;
        showPage(Math.max(shownPage, 0));
    }

    /** Authors see and filter by their own tags only; admins by every tag. */
    private void reloadTags() {
        boolean wasFilling = filling;
        filling = true;
        writingTags = app.tags().findForWriting();
        FilterOption.fill(tagFilter, scope == Scope.MINE ? "All my tags" : "All tags", writingTags, Tag::getId);
        filling = wasFilling;
    }

    @Override
    public String searchPrompt() {
        return scope == Scope.MINE ? "Search my posts by title or content" : "Search posts by title or content";
    }

    @Override
    protected void applyQuery() {
        applyFilters(true);
    }

    @Override
    protected Node root() {
        return table;
    }

    /** Admin dashboard → "View posts" of one user. */
    void filterByAuthor(int authorId) {
        filling = true;
        authorFilter.getItems().stream()
                .filter(option -> option.value() != null && option.value().getId() == authorId)
                .findFirst().ifPresent(authorFilter::setValue);
        filling = false;
        applyFilters(true);
    }

    @Override
    public void openNewForm() {
        clearForm();
        openForm(scope == Scope.MINE ? "Write a post" : "New post", form, true, titleField, this::create);
    }

    @FXML
    private void onNew() {
        openNewForm();
    }

    @FXML
    private void onFilterChanged() {
        if (!filling) {
            applyFilters(false);
        }
    }

    @FXML
    private void onResetFilters() {
        filling = true;
        statusFilter.getSelectionModel().selectFirst();
        tagFilter.getSelectionModel().selectFirst();
        authorFilter.getSelectionModel().selectFirst();
        filling = false;
        search("");
    }

    /**
     * Turns the typed text into tag chips ("Spring Boot, JavaFX" → #spring-boot #javafx). The tags
     * are only names until the post is saved; nothing is written to the database here.
     */
    @FXML
    private void onAddTag() {
        String text = newTagField.getText();
        if (text == null || text.isBlank()) {
            return;
        }
        String problem = tagProblem();
        if (problem != null) {
            errors.showField("tags", problem);   // the typed text stays, so it can be fixed
        } else {
            Arrays.stream(text.split(",")).map(TagValidator::slug).filter(slug -> !slug.isEmpty()).forEach(this::addTag);
            newTagField.clear();
            errors.clearField("tags");
        }
        newTagField.requestFocus();
    }

    /** What is wrong with the tags, counting the ones still typed in the field; {@code null} if nothing. */
    private String tagProblem() {
        List<String> slugs = tagNames().stream().map(TagValidator::slug).filter(slug -> !slug.isEmpty()).distinct().toList();
        for (String slug : slugs) {
            String problem = TagValidator.postTag(slug);
            if (problem != null) {
                return problem;
            }
        }
        return slugs.size() > TagValidator.MAX_TAGS_PER_POST ? "A post can have at most " + TagValidator.MAX_TAGS_PER_POST + " tags." : null;
    }

    private void addTag(String name) {
        if (!chosenTags.contains(name)) {
            chosenTags.add(name);
        }
    }

    /** Chips for the chosen tags (click to remove) and suggestions from the user's own tags (click to add). */
    private void renderTags() {
        List<Node> chips = new ArrayList<>();
        for (String name : chosenTags) {
            Button chip = new Button("#" + name, Icons.of(Icons.CLOSE, 10));
            chip.setContentDisplay(ContentDisplay.RIGHT);
            chip.getStyleClass().add("tag-token");
            chip.setFocusTraversable(false);
            chip.setTooltip(new Tooltip("Remove " + name));
            chip.setOnAction(e -> chosenTags.remove(name));
            chips.add(chip);
        }
        chips.add(newTagField);
        tagInput.getChildren().setAll(chips);

        List<Node> suggestions = writingTags.stream().map(Tag::getName).filter(name -> !chosenTags.contains(name))
                .map(name -> {
                    Button suggestion = new Button("+ " + name);
                    suggestion.getStyleClass().add("tag-chip");
                    suggestion.setFocusTraversable(false);
                    suggestion.setOnAction(e -> addTag(name));
                    return (Node) suggestion;
                }).toList();
        tagSuggestions.getChildren().setAll(suggestions);
        suggestionLabel.setText(scope == Scope.MINE ? "Your tags:" : "Existing tags:");
        boolean any = !suggestions.isEmpty();
        suggestionRow.setVisible(any);
        suggestionRow.setManaged(any);
    }

    /** The chosen tags plus anything still typed in the field. */
    private List<String> tagNames() {
        List<String> names = new ArrayList<>(chosenTags);
        String pending = newTagField.getText();
        if (pending != null && !pending.isBlank()) {
            names.addAll(Arrays.asList(pending.split(",")));
        }
        return names;
    }

    private void applyFilters(boolean always) {
        User author = FilterOption.selected(authorFilter);
        Tag tag = FilterOption.selected(tagFilter);
        Integer authorId = scope == Scope.MINE ? me().getId() : author == null ? null : author.getId();
        PostFilter next = new PostFilter(FilterOption.selected(statusFilter), authorId,
                tag == null ? null : tag.getId(), query());
        if (always || !next.equals(filter)) {
            filter = next;
            showPage(0);
        }
    }

    private boolean create() {
        boolean saved = save(scope == Scope.MINE ? "Your post was saved." : "Post created.",
                () -> app.posts().create(selectedAuthorId(), titleField.getText(), contentArea.getText(),
                        statusBox.getValue(), tagNames())).isPresent();
        if (saved) {
            load(this::reloadTags);   // a new tag of this author now counts as theirs
            showPage(0);   // newest first, so the new post is on the first page
        }
        return saved;
    }

    private void edit(Post post) {
        fillForm(post);
        openForm("Edit post #" + post.getId(), form, false, titleField, () -> {
            boolean saved = save("Post updated.", () -> app.posts().update(post.getId(), selectedAuthorId(),
                    titleField.getText(), contentArea.getText(), statusBox.getValue(), tagNames())).isPresent();
            if (saved) {
                load(this::reloadTags);
                showPage(shownPage);
            }
            return saved;
        });
    }

    private void togglePublished(Post post) {
        boolean publish = post.getStatus() != PostStatus.PUBLISHED;
        if (perform(publish ? "Post published. Readers can see it now." : "Post moved back to drafts.",
                () -> app.posts().changeStatus(post.getId(), publish ? PostStatus.PUBLISHED : PostStatus.DRAFT))) {
            showPage(shownPage);
        }
    }

    private void delete(Post post) {
        if (confirmDelete("\"" + post.getTitle() + "\"", "Its comments, reviews and tag links are deleted too.")
                && perform("Post deleted.", () -> app.posts().delete(post.getId()))) {
            showPage(shownPage);
        }
    }

    /** Loads one page for the current filter and syncs the table, pager and counts. */
    private void showPage(int pageIndex) {
        load(() -> {
            Page<Post> page = app.posts().findPage(filter, pageIndex, PAGE_SIZE);
            table.getItems().setAll(page.getItems());
            shownPage = page.getPageIndex();   // set first, so the page factory ignores the calls below
            pagination.setPageCount(page.getPageCount());
            pagination.setCurrentPageIndex(page.getPageIndex());
            pageInfo.setText(page.getTotalItems() == 0 ? "No posts match the filters."
                    : "Showing %d-%d of %d posts".formatted(page.getFirstItemNumber(), page.getLastItemNumber(), page.getTotalItems()));
            stats.setText(counts());
        });
    }

    /** "14 posts · 12 published · 2 drafts" for the screen's scope, ignoring the filters. */
    private String counts() {
        Integer authorId = scope == Scope.MINE ? me().getId() : null;
        long all = app.posts().findPage(new PostFilter(null, authorId, null, null), 0, 1).getTotalItems();
        long published = app.posts().findPage(new PostFilter(PostStatus.PUBLISHED, authorId, null, null), 0, 1).getTotalItems();
        return "%d posts · %d published · %d drafts".formatted(all, published, all - published);
    }

    private void fillForm(Post post) {
        errors.clear();
        authorBox.setValue(findFirst(authorBox.getItems(), user -> user.getId() == post.getAuthorId()));
        titleField.setText(post.getTitle());
        statusBox.setValue(post.getStatus());
        contentArea.setText(post.getContent());
        newTagField.clear();
        chosenTags.setAll(post.getTags().stream().map(Tag::getName).toList());
        renderTags();
    }

    private void clearForm() {
        errors.clear();
        authorBox.setValue(findFirst(authorBox.getItems(), user -> user.getId() == me().getId()));
        titleField.clear();
        statusBox.setValue(PostStatus.DRAFT);
        contentArea.clear();
        newTagField.clear();
        chosenTags.clear();
        renderTags();
    }

    private Integer selectedAuthorId() {
        if (scope == Scope.MINE) {
            return me().getId();
        }
        User author = authorBox.getValue();
        return author == null ? null : author.getId();
    }
}

package com.example.bloggingplatform.controller;

import com.example.bloggingplatform.service.AppContext;
import com.example.bloggingplatform.model.Page;
import com.example.bloggingplatform.model.Post;
import com.example.bloggingplatform.model.PostFilter;
import com.example.bloggingplatform.model.PostSort;
import com.example.bloggingplatform.model.PostStatus;
import com.example.bloggingplatform.model.Tag;
import com.example.bloggingplatform.model.User;
import com.example.bloggingplatform.util.Icons;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.Pagination;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * Discover: published posts as blog cards with author, date, reading time, excerpt, tags,
 * average rating and comment count. The search bar and the tag / author filters become a
 * {@link PostFilter}; {@code PostService.discover} takes one page from the in-memory cache, sorted
 * in the order chosen in the Sort box.
 */
public class FeedController extends BaseController {

    static final int PAGE_SIZE = 6;

    @FXML private Label summary;
    @FXML private ComboBox<FilterOption<Tag>> tagFilter;
    @FXML private ComboBox<FilterOption<User>> authorFilter;
    @FXML private ComboBox<PostSort> sortBox;
    @FXML private ScrollPane scroll;
    @FXML private VBox cards;
    @FXML private Pagination pagination;

    private PostFilter filter = new PostFilter(PostStatus.PUBLISHED, null, null, null);
    private PostSort sort = PostSort.NEWEST;
    private int shownPage = -1;
    private boolean filling;

    public FeedController(AppContext app) {
        super(app);
    }

    @FXML
    private void initialize() {
        sortBox.getItems().setAll(PostSort.values());
        sortBox.setValue(sort);
        pagination.setPageFactory(index -> {
            if (index != shownPage) {
                showPage(index);
            }
            return new Region();   // the cards are shown above; the control is only the page navigator
        });
    }

    @Override
    public void refresh() {
        filling = true;
        load(() -> {
            FilterOption.fill(tagFilter, "All tags", app.tags().findAll(), Tag::getId);
            FilterOption.fill(authorFilter, "All authors", app.users().findAuthors(), User::getId);
        });
        filling = false;
        showPage(Math.max(shownPage, 0));
    }

    @Override
    public String searchPrompt() {
        return "Search posts by keyword, tag or author";
    }

    @Override
    public void openNewForm() {
        // nothing to create here; writers use "Write a post"
    }

    @Override
    protected void applyQuery() {
        applyFilters(true);
    }

    @Override
    protected Node root() {
        return cards;
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
        tagFilter.getSelectionModel().selectFirst();
        authorFilter.getSelectionModel().selectFirst();
        sortBox.setValue(PostSort.NEWEST);
        filling = false;
        search("");
    }

    private void applyFilters(boolean always) {
        User author = FilterOption.selected(authorFilter);
        Tag tag = FilterOption.selected(tagFilter);
        PostFilter next = new PostFilter(PostStatus.PUBLISHED, author == null ? null : author.getId(),
                tag == null ? null : tag.getId(), query());
        PostSort nextSort = sortBox.getValue();
        if (always || !next.equals(filter) || nextSort != sort) {
            filter = next;
            sort = nextSort;
            showPage(0);
        }
    }

    /** Clicking a tag chip on a card shows only posts with that tag. */
    private void filterByTag(Tag tag) {
        filling = true;
        tagFilter.getItems().stream().filter(option -> tag.equals(option.value())).findFirst()
                .ifPresent(tagFilter::setValue);
        filling = false;
        applyFilters(true);
    }

    private void showPage(int pageIndex) {
        load(() -> {
            Page<Post> page = app.posts().discover(filter, sort, pageIndex, PAGE_SIZE);
            shownPage = page.getPageIndex();   // set first, so the page factory ignores the calls below
            pagination.setPageCount(page.getPageCount());
            pagination.setCurrentPageIndex(page.getPageIndex());
            cards.getChildren().setAll(page.getItems().stream().map(this::card).toList());
            if (page.getItems().isEmpty()) {
                Label empty = new Label(query().isEmpty() ? "No posts here yet." : "No posts match \"" + query() + "\".");
                empty.getStyleClass().add("empty-state");
                cards.getChildren().setAll(empty);
            }
            summary.setText(page.getTotalItems() == 0 ? "Nothing to read with these filters"
                    : "%d %s · showing %d-%d".formatted(page.getTotalItems(), query().isEmpty() ? "published posts" : "results",
                    page.getFirstItemNumber(), page.getLastItemNumber()));
            scroll.setVvalue(0);
        });
    }

    private Node card(Post post) {
        Label author = new Label(post.getAuthorName());
        author.getStyleClass().add("card-author");
        Label when = new Label("· " + BlogWidgets.date(post.getCreatedAt()) + " · " + BlogWidgets.readingTime(post.getContent()));
        when.getStyleClass().add("card-meta");
        HBox meta = new HBox(8, BlogWidgets.avatar(post.getAuthorName(), 26), author, when);
        meta.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label(post.getTitle());
        title.getStyleClass().add("card-title");
        title.setWrapText(true);
        Label excerpt = new Label(BlogWidgets.excerpt(post.getContent(), 230));
        excerpt.getStyleClass().add("card-excerpt");
        excerpt.setWrapText(true);

        FlowPane tags = new FlowPane(6, 6);
        post.getTags().forEach(tag -> tags.getChildren().add(BlogWidgets.tagChip(tag, this::filterByTag)));
        HBox.setHgrow(tags, Priority.ALWAYS);

        Label rating = new Label(post.getAverageRating() == null ? "No ratings"
                : "%s %.1f".formatted(BlogWidgets.stars(post.getAverageRating()), post.getAverageRating()));
        rating.getStyleClass().add("card-stat");
        Label comments = new Label(String.valueOf(post.getCommentCount()), Icons.of(Icons.COMMENTS, 15));
        comments.getStyleClass().add("card-stat");
        Button read = new Button("Read", Icons.of(Icons.FORWARD, 14));
        read.setContentDisplay(ContentDisplay.RIGHT);
        read.getStyleClass().add("btn-read");
        read.setOnAction(e -> navigator.openPost(post.getId()));
        HBox footer = new HBox(14, tags, rating, comments, read);
        footer.setAlignment(Pos.CENTER_LEFT);

        VBox card = new VBox(meta, title, excerpt, footer);
        card.getStyleClass().add("post-card");
        card.setOnMouseClicked(e -> {
            if (!insideButton(e.getPickResult().getIntersectedNode(), card)) {   // tag chips and Read handle themselves
                navigator.openPost(post.getId());
            }
        });
        return card;
    }

    private static boolean insideButton(Node node, Node stop) {
        for (Node n = node; n != null && n != stop; n = n.getParent()) {
            if (n instanceof Button) {
                return true;
            }
        }
        return false;
    }
}

package com.example.bloggingplatform.controller;

import com.example.bloggingplatform.service.AppContext;
import com.example.bloggingplatform.model.User;
import com.example.bloggingplatform.validations.PostValidator;
import com.example.bloggingplatform.config.DBConnection;
import com.example.bloggingplatform.util.Icons;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The signed-in workspace. The sidebar depends on the role:
 * <ul>
 *   <li><b>Reader</b>: Discover (read, comment, rate), My comments, My reviews.</li>
 *   <li><b>Author</b>: Write a post, Discover, My posts, Comments and Reviews on my posts.</li>
 *   <li><b>Admin</b>: Write a post, Dashboard, Discover, and every table: Posts, Comments,
 *       Reviews, Tags, Users.</li>
 * </ul>
 * Screens are loaded the first time they are opened and kept afterwards. The search bar at the
 * top always searches the screen that is showing.
 */
public class ShellController implements Navigator {

    /** One sidebar entry: which view it opens and, for shared views, whose records it shows. */
    private record Entry(String key, String label, String icon, String fxml, Scope scope) {
    }

    private static final String ARTICLE = "article";
    private static final Pattern JDBC_URL = Pattern.compile("//([^/?;]+)/([^?;]+)");

    @FXML private VBox navBox;
    @FXML private Label connectionLabel;
    @FXML private Button exitButton;
    @FXML private StackPane searchIcon;
    @FXML private TextField searchField;
    @FXML private Button clearSearchButton;
    @FXML private Button signOutButton;
    @FXML private Label avatarLetter;
    @FXML private Label userName;
    @FXML private Label userRole;
    @FXML private StackPane screenHost;

    private final AppContext app;
    private final User user;
    private final ToggleGroup navigation = new ToggleGroup();
    private final Map<String, Entry> entries = new LinkedHashMap<>();
    private final Map<String, ToggleButton> navButtons = new HashMap<>();
    private final Map<String, Views.View<BaseController>> screens = new HashMap<>();
    private BaseController current;
    private String currentKey;
    /** Screen to go back to from the reading view. */
    private String returnKey;
    private Runnable onSignOut = () -> { };

    public ShellController(AppContext app) {
        this.app = app;
        this.user = app.session().require();
    }

    @FXML
    private void initialize() {
        searchIcon.getChildren().setAll(Icons.of(Icons.SEARCH, 17));
        clearSearchButton.setGraphic(Icons.of(Icons.CLOSE, 12));
        clearSearchButton.visibleProperty().bind(searchField.textProperty().isNotEmpty());
        clearSearchButton.managedProperty().bind(clearSearchButton.visibleProperty());
        signOutButton.setGraphic(Icons.of(Icons.SIGN_OUT, 15));
        exitButton.setGraphic(Icons.of(Icons.EXIT, 16));
        avatarLetter.setText(user.getUsername().substring(0, 1).toUpperCase(Locale.ROOT));
        userName.setText(user.getUsername());
        userRole.setText(user.getRole().name());
        connectionLabel.setText("● " + describe(DBConnection.URL));
        connectionLabel.setTooltip(new Tooltip("Connected to " + DBConnection.URL));
        FormErrors.limit(searchField, PostValidator.SEARCH_MAX);
        buildSidebar();
    }

    public void setOnSignOut(Runnable onSignOut) {
        this.onSignOut = onSignOut;
    }

    /** Opens the first screen and installs the keyboard shortcuts on the window's scene. */
    public void start(Scene scene) {
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.F, KeyCombination.SHORTCUT_DOWN), () -> {
            searchField.requestFocus();
            searchField.selectAll();
        });
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.N, KeyCombination.SHORTCUT_DOWN),
                () -> current.openNewForm());
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.F5), () -> current.refresh());
        show(entries.keySet().iterator().next());
        Dialogs.toast(scene.getWindow(), "Signed in as " + user.getUsername() + " (" + user.getRole() + ")");
    }

    @Override
    public void openPost(int postId) {
        if (!ARTICLE.equals(currentKey)) {
            returnKey = currentKey;
        }
        Views.View<BaseController> article = screen(ARTICLE, "article-view.fxml", null);
        ((ArticleController) article.controller()).setPost(postId);
        display(ARTICLE, article);
    }

    @Override
    public void back() {
        show(returnKey != null ? returnKey : entries.keySet().iterator().next());
    }

    @Override
    public void showPostsOf(int authorId) {
        show("posts");
        ((PostController) current).filterByAuthor(authorId);
    }

    @FXML
    private void onSearch() {
        String text = searchField.getText();
        if (ARTICLE.equals(currentKey)) {
            show("discover");   // searching while reading goes back to the post list
        }
        current.search(text);
    }

    @FXML
    private void onClearSearch() {
        searchField.clear();
        current.search("");
        searchField.requestFocus();
    }

    @FXML
    private void onSignOut() {
        onSignOut.run();
    }

    @FXML
    private void onExit() {
        Platform.exit();
    }

    private void buildSidebar() {
        switch (user.getRole()) {
            case READER -> {
                section("READ");
                entry(new Entry("discover", "Discover", Icons.DISCOVER, "feed-view.fxml", null));
                section("MY ACTIVITY");
                entry(new Entry("my-comments", "My comments", Icons.COMMENTS, "comments-view.fxml", Scope.MINE));
                entry(new Entry("my-reviews", "My reviews", Icons.REVIEWS, "reviews-view.fxml", Scope.MINE));
            }
            case AUTHOR -> {
                writeButton("my-posts");
                section("READ");
                entry(new Entry("discover", "Discover", Icons.DISCOVER, "feed-view.fxml", null));
                section("MY BLOG");
                entry(new Entry("my-posts", "My posts", Icons.POSTS, "posts-view.fxml", Scope.MINE));
                entry(new Entry("comments", "Comments", Icons.COMMENTS, "comments-view.fxml",
                        Scope.ON_MY_POSTS));
                entry(new Entry("reviews", "Reviews", Icons.REVIEWS, "reviews-view.fxml",
                        Scope.ON_MY_POSTS));
            }
            case ADMIN -> {
                writeButton("posts");
                section("OVERVIEW");
                entry(new Entry("dashboard", "Dashboard", Icons.DASHBOARD, "reports-view.fxml", null));
                entry(new Entry("discover", "Discover", Icons.DISCOVER, "feed-view.fxml", null));
                section("MANAGE");
                entry(new Entry("posts", "Posts", Icons.POSTS, "posts-view.fxml", Scope.ALL));
                entry(new Entry("comments", "Comments", Icons.COMMENTS, "comments-view.fxml", Scope.ALL));
                entry(new Entry("reviews", "Reviews", Icons.REVIEWS, "reviews-view.fxml", Scope.ALL));
                entry(new Entry("tags", "Tags", Icons.TAGS, "tags-view.fxml", null));
                entry(new Entry("users", "Users", Icons.USERS, "users-view.fxml", null));
            }
        }
    }

    /** The green "Write a post" button: opens the post table and its "New post" pop-up. */
    private void writeButton(String postsKey) {
        Button write = new Button("Write a post", Icons.of(Icons.WRITE, 16));
        write.getStyleClass().addAll("btn-create", "sidebar-write");
        write.setMaxWidth(Double.MAX_VALUE);
        write.setOnAction(e -> {
            show(postsKey);
            current.openNewForm();
        });
        navBox.getChildren().add(write);
    }

    private void section(String title) {
        Label label = new Label(title);
        label.getStyleClass().add("sidebar-section");
        navBox.getChildren().add(label);
    }

    private void entry(Entry entry) {
        entries.put(entry.key(), entry);
        ToggleButton button = new ToggleButton(entry.label(), Icons.of(entry.icon(), 18));
        button.getStyleClass().add("nav-button");
        button.setMaxWidth(Double.MAX_VALUE);
        button.setToggleGroup(navigation);
        button.setOnAction(e -> show(entry.key()));   // also when it is already selected, e.g. coming back from a post
        navButtons.put(entry.key(), button);
        navBox.getChildren().add(button);
    }

    private void show(String key) {
        Entry entry = entries.get(key);
        display(key, screen(key, entry.fxml(), entry.scope()));
        navigation.selectToggle(navButtons.get(key));
    }

    /** The screen's view, loaded on first use. */
    private Views.View<BaseController> screen(String key, String fxml, Scope scope) {
        return screens.computeIfAbsent(key, k -> {
            Views.View<BaseController> view = Views.load(fxml, app);
            BaseController controller = view.controller();
            controller.setNavigator(this);
            if (scope != null) {
                controller.setScope(scope);
            }
            // a screen can clear its own search (e.g. "Reset filters"); keep the search bar in step
            controller.queryProperty().addListener((obs, old, query) -> {
                if (controller == current) {
                    searchField.setText(query);
                }
            });
            return view;
        });
    }

    private void display(String key, Views.View<BaseController> view) {
        current = view.controller();
        currentKey = key;
        screenHost.getChildren().setAll(view.root());
        searchField.setPromptText(current.searchPrompt());
        searchField.setText(current.query());
        current.refresh();
        FadeTransition fade = new FadeTransition(Duration.millis(160), view.root());
        fade.setFromValue(0.4);
        fade.setToValue(1);
        fade.play();
    }

    /** "bloggingPlatform @ localhost:3306" from the JDBC URL. */
    private static String describe(String url) {
        Matcher matcher = JDBC_URL.matcher(url);
        return matcher.find() ? matcher.group(2) + " @ " + matcher.group(1) : url;
    }
}

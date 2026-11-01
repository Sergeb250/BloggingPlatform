package com.example.bloggingplatform.controller;

import com.example.bloggingplatform.service.AppContext;
import com.example.bloggingplatform.util.Icons;
import com.example.bloggingplatform.util.NotebookArt;
import com.example.bloggingplatform.util.WindowFrame;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.util.Duration;

/**
 * The window: title bar with minimize / maximize / close, and the page area, which shows the
 * sign-in page until someone signs in and then the workspace for their role.
 */
public class MainController {

    /** Smallest window size (including the shadow margin) that still shows the tables well. */
    private static final double MIN_WIDTH = 1040;
    private static final double MIN_HEIGHT = 660;

    @FXML private StackPane windowRoot;
    @FXML private HBox titleBar;
    @FXML private StackPane logoBox;
    @FXML private Button minimizeButton;
    @FXML private Button maximizeButton;
    @FXML private Tooltip maximizeTip;
    @FXML private Button closeButton;
    @FXML private StackPane page;

    private final AppContext app;
    private Stage stage;
    private WindowFrame frame;

    public MainController(AppContext app) {
        this.app = app;
    }

    @FXML
    private void initialize() {
        logoBox.getChildren().setAll(NotebookArt.logo(22));
        minimizeButton.setGraphic(Icons.of(Icons.MINIMIZE, 14));
        maximizeButton.setGraphic(Icons.of(Icons.MAXIMIZE, 14));
        closeButton.setGraphic(Icons.of(Icons.CLOSE, 14));
    }

    /** Connects the controller to its stage: window behaviour, starting size and the first page. */
    public void attach(Stage stage) {
        this.stage = stage;
        stage.setMinWidth(MIN_WIDTH);
        stage.setMinHeight(MIN_HEIGHT);
        frame = WindowFrame.install(stage, windowRoot, titleBar);
        frame.maximizedProperty().addListener((obs, was, maximized) -> {
            maximizeButton.setGraphic(Icons.of(maximized ? Icons.RESTORE : Icons.MAXIMIZE, 14));
            maximizeTip.setText(maximized ? "Restore" : "Maximize");
        });
        frame.sizeToScreen(0.86, 1320, 820);   // not full screen on first open
        showSignIn();
    }

    private void showSignIn() {
        Views.View<LoginController> login = Views.load("login-view.fxml", app);
        login.controller().setOnSignedIn(this::showWorkspace);
        setPage(login.root());
    }

    private void showWorkspace() {
        Views.View<ShellController> shell = Views.load("shell-view.fxml", app);
        shell.controller().setOnSignOut(() -> {
            app.auth().logout();
            stage.getScene().getAccelerators().clear();
            showSignIn();
        });
        setPage(shell.root());
        shell.controller().start(stage.getScene());
    }

    private void setPage(Node content) {
        page.getChildren().setAll(content);
        FadeTransition fade = new FadeTransition(Duration.millis(220), content);
        fade.setFromValue(0);
        fade.setToValue(1);
        fade.play();
    }

    @FXML
    private void onMinimize() {
        frame.minimize();
    }

    @FXML
    private void onMaximize() {
        frame.toggleMaximize();
    }

    @FXML
    private void onClose() {
        Platform.exit();
    }
}

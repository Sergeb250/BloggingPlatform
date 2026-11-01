package com.example.bloggingplatform.util;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.geometry.Insets;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import javafx.util.Duration;

import java.util.List;

/**
 * A modal pop-up window in the application's style: a rectangular white card with a black
 * border, a light title bar and a row of buttons at the bottom right.
 * <ul>
 *   <li>It never exceeds the screen: if its content is taller or wider than the usable screen
 *       area, the pop-up is shrunk to fit and its body scrolls.</li>
 *   <li>Drag the title bar to move it (it stays on the screen); drag any edge or corner to
 *       resize it. Esc or ✕ closes it.</li>
 * </ul>
 */
public final class PopupWindow {

    /** Transparent margin around the card that holds its shadow and the resize grip. */
    private static final double SHADOW = 14;
    /** Space kept free between the pop-up and the screen edges. */
    private static final double SCREEN_MARGIN = 16;
    private static final double MIN_WIDTH = 360;
    private static final double MIN_HEIGHT = 220;

    private final Stage stage = new Stage(StageStyle.TRANSPARENT);
    private final VBox card;
    private final StackPane body = new StackPane();
    private final HBox buttons = new HBox();

    /** @param owner the window the pop-up belongs to and is centred on, or {@code null} */
    public PopupWindow(Window owner, String title) {
        if (owner != null) {
            stage.initOwner(owner);
            stage.initModality(Modality.WINDOW_MODAL);
        } else {
            stage.initModality(Modality.APPLICATION_MODAL);
        }
        stage.setTitle(title);
        stage.setMinWidth(MIN_WIDTH);
        stage.setMinHeight(MIN_HEIGHT);

        Label heading = new Label(title);
        heading.getStyleClass().add("popup-title");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Button close = new Button();
        close.setGraphic(Icons.of(Icons.CLOSE, 14));
        close.getStyleClass().addAll("window-button", "window-close");
        close.setFocusTraversable(false);
        close.setOnAction(e -> close());
        HBox titleBar = new HBox(heading, spacer, close);
        titleBar.getStyleClass().add("popup-title-bar");
        makeMovable(titleBar);

        body.getStyleClass().add("popup-body");
        ScrollPane scroll = new ScrollPane(body);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.getStyleClass().add("popup-scroll");
        buttons.getStyleClass().add("popup-buttons");
        card = new VBox(titleBar, scroll, buttons);
        card.getStyleClass().add("popup-card");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        StackPane root = new StackPane(card);
        root.getStyleClass().add("popup-root");
        root.setPadding(new Insets(SHADOW));
        WindowFrame.enableResizing(stage, root, SHADOW + 5, () -> false);
        Scene scene = new Scene(root, Color.TRANSPARENT);
        Theme.apply(scene);
        scene.addEventHandler(KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == KeyCode.ESCAPE) {
                e.consume();
                close();
            }
        });
        stage.setScene(scene);
    }

    public void setContent(Node content) {
        if (content == null) {
            body.getChildren().clear();
        } else {
            body.getChildren().setAll(content);
        }
    }

    /** Adds a button to the bottom row; {@code styleClass} may be {@code null} for a plain black button. */
    public Button addButton(String text, String styleClass, Runnable action) {
        Button button = new Button(text);
        if (styleClass != null) {
            button.getStyleClass().add(styleClass);
        }
        button.setMinWidth(Region.USE_PREF_SIZE);
        button.setOnAction(e -> action.run());
        buttons.getChildren().add(button);
        return button;
    }

    /** Runs {@code action} once the pop-up has closed, however it was closed. */
    public void setOnClosed(Runnable action) {
        stage.setOnHidden(e -> action.run());
    }

    /** Shows the pop-up and waits until it is closed. */
    public void showAndWait(Node initialFocus) {
        // Measure, fit and centre only once the pop-up is on screen: sizes measured earlier can be
        // off on high-DPI screens and cut button labels short. It stays invisible until placed.
        stage.setOpacity(0);
        stage.setOnShown(e -> {
            fitAndCentre();
            stage.setOpacity(1);
            playOpenAnimation();
            if (initialFocus != null) {
                initialFocus.requestFocus();
            }
        });
        stage.showAndWait();
    }

    public void close() {
        stage.close();
    }

    public Stage stage() {
        return stage;
    }

    /** Shrinks the pop-up to the usable screen area if needed, then centres it on its owner. */
    private void fitAndCentre() {
        Rectangle2D screen = usableArea();
        stage.setWidth(Math.min(stage.getWidth(), screen.getWidth()));
        stage.setHeight(Math.min(stage.getHeight(), screen.getHeight()));
        Window owner = stage.getOwner();
        Rectangle2D area = owner == null || !owner.isShowing() ? screen
                : new Rectangle2D(owner.getX(), owner.getY(), owner.getWidth(), owner.getHeight());
        moveTo(area.getMinX() + (area.getWidth() - stage.getWidth()) / 2,
                area.getMinY() + (area.getHeight() - stage.getHeight()) / 2);
    }

    /** Dragging the title bar moves the pop-up, but never off the screen. */
    private void makeMovable(Node handle) {
        double[] offset = new double[2];
        handle.setOnMousePressed(e -> {
            offset[0] = stage.getX() - e.getScreenX();
            offset[1] = stage.getY() - e.getScreenY();
        });
        handle.setOnMouseDragged(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                moveTo(e.getScreenX() + offset[0], e.getScreenY() + offset[1]);
            }
        });
    }

    /** Places the pop-up at (x, y), pushed back inside the usable screen area. */
    private void moveTo(double x, double y) {
        Rectangle2D screen = usableArea();
        // the shadow margin may hang over the screen edge; the card itself stays on screen
        stage.setX(Math.clamp(x, screen.getMinX() - SHADOW, Math.max(screen.getMinX() - SHADOW,
                screen.getMaxX() + SHADOW - stage.getWidth())));
        stage.setY(Math.clamp(y, screen.getMinY() - SHADOW, Math.max(screen.getMinY() - SHADOW,
                screen.getMaxY() + SHADOW - stage.getHeight())));
    }

    /** The visual bounds (screen minus taskbar) of the screen the owner is on, minus a margin. */
    private Rectangle2D usableArea() {
        Window owner = stage.getOwner();
        List<Screen> screens = owner == null ? List.of()
                : Screen.getScreensForRectangle(owner.getX(), owner.getY(), owner.getWidth(), owner.getHeight());
        Rectangle2D bounds = (screens.isEmpty() ? Screen.getPrimary() : screens.getFirst()).getVisualBounds();
        double margin = SCREEN_MARGIN - SHADOW;   // the transparent shadow may use part of the margin
        return new Rectangle2D(bounds.getMinX() + margin, bounds.getMinY() + margin,
                bounds.getWidth() - 2 * margin, bounds.getHeight() - 2 * margin);
    }

    private void playOpenAnimation() {
        FadeTransition fade = new FadeTransition(Duration.millis(160), card);
        fade.setFromValue(0);
        fade.setToValue(1);
        ScaleTransition grow = new ScaleTransition(Duration.millis(160), card);
        grow.setFromX(0.96);
        grow.setFromY(0.96);
        grow.setToX(1);
        grow.setToY(1);
        grow.setInterpolator(Interpolator.EASE_OUT);
        new ParallelTransition(fade, grow).play();
    }
}

package com.example.bloggingplatform.util;

import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.TranslateTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Window;
import javafx.util.Duration;

/**
 * A short success message that slides up at the bottom of a window and fades away by itself,
 * so a successful save needs no extra click. Shown on top of the window's root, which must be a
 * {@link StackPane}.
 */
public final class Toast {

    private static final String STYLE = "toast";
    private static final Duration VISIBLE = Duration.millis(2400);

    private Toast() {
    }

    public static void show(Window owner, String message) {
        if (owner == null || owner.getScene() == null || !(owner.getScene().getRoot() instanceof StackPane layer)) {
            return;
        }
        layer.getChildren().removeIf(node -> node.getStyleClass().contains(STYLE));   // one toast at a time

        Label toast = new Label(message, Icons.of(Icons.CHECK, 16));
        toast.getStyleClass().add(STYLE);
        toast.setMouseTransparent(true);
        toast.setOpacity(0);
        StackPane.setAlignment(toast, Pos.BOTTOM_CENTER);
        StackPane.setMargin(toast, new Insets(0, 0, 34, 0));
        layer.getChildren().add(toast);

        FadeTransition fadeIn = new FadeTransition(Duration.millis(180), toast);
        fadeIn.setToValue(1);
        TranslateTransition slideUp = new TranslateTransition(Duration.millis(180), toast);
        slideUp.setFromY(12);
        slideUp.setToY(0);
        FadeTransition fadeOut = new FadeTransition(Duration.millis(320), toast);
        fadeOut.setToValue(0);
        SequentialTransition life = new SequentialTransition(
                new ParallelTransition(fadeIn, slideUp), new PauseTransition(VISIBLE), fadeOut);
        life.setOnFinished(e -> layer.getChildren().remove(toast));
        life.play();
    }
}

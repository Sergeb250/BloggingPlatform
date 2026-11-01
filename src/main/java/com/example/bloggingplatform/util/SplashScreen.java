package com.example.bloggingplatform.util;

import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;

/**
 * Start-up window shown while the database connection is checked: the notebook-and-pen
 * animation, the app name, and a status line with a bouncing three-dot loader.
 */
public final class SplashScreen {

    private final Stage stage = new Stage(StageStyle.TRANSPARENT);
    private final VBox card;
    private final Label status = new Label();
    private final NotebookArt.Animated notebook = NotebookArt.writingNotebook();
    private final List<Animation> dots = new ArrayList<>();

    public SplashScreen(List<Image> icons) {
        Label title = new Label("Smart Blogging Platform");
        title.getStyleClass().add("splash-title");
        Label subtitle = new Label("Write  ·  Comment  ·  Review");
        subtitle.getStyleClass().add("splash-subtitle");
        status.getStyleClass().add("splash-status");

        HBox loader = new HBox(6);
        loader.setAlignment(Pos.CENTER);
        for (int i = 0; i < 3; i++) {
            Circle dot = new Circle(3.2);
            dot.getStyleClass().add("splash-dot");
            loader.getChildren().add(dot);
            dots.add(bounce(dot, i));
        }
        HBox loading = new HBox(10, status, loader);
        loading.getStyleClass().add("splash-loading");

        card = new VBox(notebook.view(), title, subtitle, loading);
        card.getStyleClass().add("splash-card");
        StackPane root = new StackPane(card);
        root.getStyleClass().add("splash-root");
        Scene scene = new Scene(root, Color.TRANSPARENT);
        Theme.apply(scene);
        stage.setScene(scene);
        stage.setTitle("Smart Blogging Platform");
        stage.getIcons().setAll(icons);
        setStatus("Connecting to the database");
    }

    public void show() {
        card.setOpacity(0);
        stage.show();
        stage.centerOnScreen();
        notebook.play();
        dots.forEach(Animation::play);
        FadeTransition fadeIn = new FadeTransition(Duration.millis(300), card);
        fadeIn.setToValue(1);
        fadeIn.play();
    }

    public void setStatus(String text) {
        status.setText(text);
    }

    /** Stops the loader and shows why start-up stopped; the splash stays open behind the error. */
    public void fail(String text) {
        stopAnimations();
        setStatus(text);
    }

    /** Fades the splash out, then closes it. */
    public void close() {
        FadeTransition fadeOut = new FadeTransition(Duration.millis(260), card);
        fadeOut.setToValue(0);
        fadeOut.setOnFinished(e -> {
            stopAnimations();
            stage.close();
        });
        fadeOut.play();
    }

    private void stopAnimations() {
        notebook.stop();
        dots.forEach(Animation::stop);
    }

    /** One dot of the loader: rises and brightens, falls and dims; each dot starts a little later. */
    private static Animation bounce(Circle dot, int index) {
        Timeline bounce = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(dot.translateYProperty(), 0), new KeyValue(dot.opacityProperty(), 0.3)),
                new KeyFrame(Duration.millis(260),
                        new KeyValue(dot.translateYProperty(), -6, Interpolator.EASE_OUT),
                        new KeyValue(dot.opacityProperty(), 1)),
                new KeyFrame(Duration.millis(520),
                        new KeyValue(dot.translateYProperty(), 0, Interpolator.EASE_IN),
                        new KeyValue(dot.opacityProperty(), 0.3)),
                new KeyFrame(Duration.millis(1000)));
        bounce.setCycleCount(Animation.INDEFINITE);
        bounce.setDelay(Duration.millis(index * 160));
        return bounce;
    }
}

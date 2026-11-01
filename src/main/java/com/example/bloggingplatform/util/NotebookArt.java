package com.example.bloggingplatform.util;

import javafx.animation.Animation;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.beans.value.WritableValue;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.SnapshotParameters;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Paint;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Polyline;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Translate;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

/**
 * The notebook-and-pen artwork, drawn with JavaFX shapes: the splash animation (a pen writing
 * lines of "handwriting" on a spiral notebook page), the small logo in the title bar and the
 * window icons.
 */
public final class NotebookArt {

    /** A drawing and the animations that bring it to life; call {@link #stop()} once it is hidden. */
    public record Animated(Pane view, List<Animation> animations) {

        public void play() {
            animations.forEach(Animation::play);
        }

        public void stop() {
            animations.forEach(Animation::stop);
        }
    }

    /** Glossy black for the taskbar icon tile (the icon is one of the few black surfaces). */
    static final Paint ICON_BLACK = new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
            new Stop(0, Color.web("#3b3b3b")), new Stop(0.46, Color.web("#161616")),
            new Stop(0.54, Color.BLACK), new Stop(1, Color.web("#0b0b0b")));
    private static final Paint SILVER = new LinearGradient(0, 0, 1, 0, true, CycleMethod.NO_CYCLE,
            new Stop(0, Color.web("#8f8f8f")), new Stop(0.35, Color.WHITE),
            new Stop(0.7, Color.web("#d4d4d4")), new Stop(1, Color.web("#7d7d7d")));
    private static final Paint GRIP = new LinearGradient(0, 0, 1, 0, true, CycleMethod.NO_CYCLE,
            new Stop(0, Color.BLACK), new Stop(0.45, Color.web("#4d4d4d")), new Stop(1, Color.BLACK));

    // geometry of the splash drawing
    private static final double WIDTH = 270;
    private static final double HEIGHT = 222;
    private static final double PAGE_X = 72;
    private static final double PAGE_Y = 52;
    private static final double PAGE_W = 134;
    private static final double PAGE_H = 160;
    private static final double FIRST_RULE = PAGE_Y + 30;
    private static final double RULE_GAP = 22;
    private static final double TEXT_X = PAGE_X + 28;   // writing starts right of the spiral
    /** Widths of the "words" on each handwritten line. */
    private static final double[][] WORDS = {{30, 20, 26}, {22, 34, 28}, {36, 30}, {18, 26, 36}, {24, 26}};
    private static final double WORD_GAP = 5;
    private static final double MS_PER_PIXEL = 9;   // writing speed

    private NotebookArt() {
    }

    /** The splash illustration: a pen writing five lines, erasing them and starting again. */
    public static Animated writingNotebook() {
        Rectangle backPage = rectangle(PAGE_X + 7, PAGE_Y + 6, PAGE_W, PAGE_H, 14, Color.web("#e6e3df"));
        Rectangle page = rectangle(PAGE_X, PAGE_Y, PAGE_W, PAGE_H, 14, Color.WHITE);
        page.setStroke(Color.BLACK);   // outlined, so the page shows on the white splash card
        page.setStrokeWidth(1.6);
        Group drawing = new Group(backPage, page);

        for (int i = 0; i < 6; i++) {
            double y = FIRST_RULE + i * RULE_GAP;
            drawing.getChildren().add(line(PAGE_X + 22, y, PAGE_X + PAGE_W - 12, y, Color.rgb(0, 0, 0, 0.16), 1));
        }
        for (int i = 0; i < 7; i++) {   // spiral binding: a wire loop over the edge into each hole
            double y = PAGE_Y + 18 + i * 21;
            Rectangle ring = rectangle(PAGE_X - 10, y - 3, 22, 6, 6, Color.WHITE);
            ring.setStroke(Color.BLACK);
            ring.setStrokeWidth(1.4);
            drawing.getChildren().addAll(ring, new Circle(PAGE_X + 10, y, 2.3, Color.BLACK));
        }

        Group ink = new Group();
        List<Rectangle> reveals = new ArrayList<>();
        for (int i = 0; i < WORDS.length; i++) {
            Group handwriting = handwriting(WORDS[i], TEXT_X, baseline(i), i);
            Rectangle reveal = new Rectangle(TEXT_X - 3, baseline(i) - 14, 0, 22);
            handwriting.setClip(reveal);   // the line appears as the clip widens behind the pen
            reveals.add(reveal);
            ink.getChildren().add(handwriting);
        }
        drawing.getChildren().add(ink);

        Group penBody = pen();
        Group pen = new Group(penBody);
        drawing.getChildren().add(pen);

        Timeline writing = writingTimeline(pen, ink, reveals);
        TranslateTransition scribble = new TranslateTransition(Duration.millis(80), penBody);
        scribble.setFromY(0);
        scribble.setToY(-1.8);
        scribble.setAutoReverse(true);
        scribble.setCycleCount(Animation.INDEFINITE);

        Pane view = new Pane(drawing);
        view.setMinSize(WIDTH, HEIGHT);
        view.setPrefSize(WIDTH, HEIGHT);
        view.setMaxSize(WIDTH, HEIGHT);
        return new Animated(view, List.of(writing, scribble));
    }

    /** The app logo, {@code size} pixels square: a notebook page with a pen. */
    public static Node logo(double size) {
        Group shapes = logoShapes();
        shapes.getTransforms().add(new Scale(size / 24, size / 24, 0, 0));
        Pane box = new Pane(shapes);
        box.setMinSize(size, size);
        box.setPrefSize(size, size);
        box.setMaxSize(size, size);
        return box;
    }

    /** Window icons (taskbar, Alt+Tab): the logo on a rounded black tile, in several sizes. */
    public static List<Image> icons() {
        return IntStream.of(16, 24, 32, 48, 64, 128, 256).mapToObj(NotebookArt::icon).toList();
    }

    private static Image icon(int size) {
        Rectangle tile = rectangle(0, 0, size, size, size * 0.42, ICON_BLACK);
        Group logo = logoShapes();
        double scale = size * 0.78 / 24;
        logo.getTransforms().addAll(new Translate(size * 0.11, size * 0.11), new Scale(scale, scale, 0, 0));
        SnapshotParameters parameters = new SnapshotParameters();
        parameters.setFill(Color.TRANSPARENT);
        return new Group(tile, logo).snapshot(parameters, null);
    }

    /** Logo drawn on a 24 × 24 grid. */
    private static Group logoShapes() {
        Rectangle page = rectangle(6.5, 2.5, 13.5, 19, 3.2, Color.WHITE);
        page.setStroke(Color.BLACK);
        page.setStrokeWidth(1.2);
        Group logo = new Group(page);
        for (double y : new double[]{8, 11.5, 15}) {
            logo.getChildren().add(line(10, y, y == 15 ? 14.5 : 17.5, y, Color.BLACK, 1.1));
        }
        for (double y : new double[]{5.5, 9.5, 13.5, 17.5}) {
            Rectangle ring = rectangle(3.6, y - 1, 5.6, 2, 2, Color.WHITE);
            ring.setStroke(Color.BLACK);
            ring.setStrokeWidth(0.7);
            logo.getChildren().add(ring);
        }
        Rectangle barrel = rectangle(-1.5, -13, 3, 10.5, 1.5, Color.BLACK);
        barrel.setStroke(Color.WHITE);
        barrel.setStrokeWidth(0.7);
        Polygon nib = new Polygon(0, 0, -1.5, -2.8, 1.5, -2.8);
        nib.setFill(Color.BLACK);
        Group pen = new Group(barrel, nib);
        pen.getTransforms().addAll(new Translate(15.8, 20.5), new Rotate(36, 0, 0));
        logo.getChildren().add(pen);
        return logo;
    }

    /** A fountain pen with its tip at (0, 0), leaning right like a right-handed writer's. */
    private static Group pen() {
        Polygon nib = new Polygon(0, 0, -4.2, -11.5, 4.2, -11.5);
        nib.setFill(Color.BLACK);
        Line slit = line(0, -2.5, 0, -8.5, Color.WHITE, 0.8);
        Rectangle grip = rectangle(-4.6, -24, 9.2, 12.8, 2, GRIP);
        Rectangle barrel = rectangle(-5, -80, 10, 57, 4, SILVER);
        barrel.setStroke(Color.BLACK);
        barrel.setStrokeWidth(0.8);
        Rectangle band = rectangle(-5, -66, 10, 3, 0, Color.BLACK);
        Rectangle cap = rectangle(-5.6, -93, 11.2, 15, 5, SILVER);
        cap.setStroke(Color.BLACK);
        cap.setStrokeWidth(0.8);
        Rectangle clip = rectangle(4.4, -91, 2.6, 27, 2, Color.BLACK);
        clip.setStroke(Color.WHITE);
        clip.setStrokeWidth(0.6);

        Group pen = new Group(barrel, band, cap, clip, grip, nib, slit);
        pen.getTransforms().add(new Rotate(36, 0, 0));
        pen.setEffect(new DropShadow(6, 3, 4, Color.rgb(0, 0, 0, 0.35)));
        return pen;
    }

    /** One line of scribbled "words" sitting on {@code baseline}. */
    private static Group handwriting(double[] words, double startX, double baseline, int seed) {
        Group line = new Group();
        double x = startX;
        for (int w = 0; w < words.length; w++) {
            Polyline word = new Polyline();
            double phase = seed * 1.7 + w * 2.3;
            for (double dx = 0; dx <= words[w]; dx += 0.8) {
                double rise = 3.2 + 2.3 * Math.sin(dx * 0.95 + phase) + 1.1 * Math.sin(dx * 0.33 + phase * 1.6);
                if (((int) (dx / 7 + phase)) % 5 == 0) {
                    rise += 3.5 * Math.abs(Math.sin(dx * 0.45));   // an occasional tall letter
                }
                word.getPoints().addAll(x + dx, baseline - rise);
            }
            word.setStroke(Color.BLACK);
            word.setStrokeWidth(1.5);
            word.setStrokeLineCap(StrokeLineCap.ROUND);
            word.setStrokeLineJoin(StrokeLineJoin.ROUND);
            word.setFill(null);
            line.getChildren().add(word);
            x += words[w] + WORD_GAP;
        }
        return line;
    }

    /**
     * Pen and ink in one timeline: write each line (the reveal clip widens while the pen moves
     * along), hop to the next line, then hold, fade the ink out and start again.
     */
    private static Timeline writingTimeline(Group pen, Group ink, List<Rectangle> reveals) {
        List<KeyFrame> frames = new ArrayList<>();
        List<KeyValue> start = new ArrayList<>(List.of(
                value(pen.translateXProperty(), TEXT_X), value(pen.translateYProperty(), baseline(0)),
                value(ink.opacityProperty(), 1.0)));
        reveals.forEach(reveal -> start.add(value(reveal.widthProperty(), 0.0)));
        frames.add(new KeyFrame(Duration.ZERO, start.toArray(KeyValue[]::new)));

        double time = 0;
        double endX = TEXT_X;
        for (int i = 0; i < reveals.size(); i++) {
            double y = baseline(i);
            if (i > 0) {
                time += 280;   // lift the pen and move to the start of the next line
                frames.add(new KeyFrame(Duration.millis(time),
                        new KeyValue(pen.translateXProperty(), TEXT_X, Interpolator.EASE_BOTH),
                        new KeyValue(pen.translateYProperty(), y, Interpolator.EASE_BOTH)));
            }
            Rectangle reveal = reveals.get(i);
            frames.add(new KeyFrame(Duration.millis(time), value(reveal.widthProperty(), 0.0)));
            double length = lineLength(WORDS[i]);
            endX = TEXT_X + length;
            time += length * MS_PER_PIXEL;
            frames.add(new KeyFrame(Duration.millis(time), value(reveal.widthProperty(), length + 6),
                    value(pen.translateXProperty(), endX), value(pen.translateYProperty(), y)));
        }
        double lastY = baseline(reveals.size() - 1);
        time += 700;   // admire the page
        frames.add(new KeyFrame(Duration.millis(time), value(ink.opacityProperty(), 1.0),
                value(pen.translateXProperty(), endX), value(pen.translateYProperty(), lastY)));
        time += 380;   // erase and go back to the top
        frames.add(new KeyFrame(Duration.millis(time), value(ink.opacityProperty(), 0.0),
                new KeyValue(pen.translateXProperty(), TEXT_X, Interpolator.EASE_BOTH),
                new KeyValue(pen.translateYProperty(), baseline(0), Interpolator.EASE_BOTH)));

        Timeline timeline = new Timeline(frames.toArray(KeyFrame[]::new));
        timeline.setCycleCount(Animation.INDEFINITE);
        return timeline;
    }

    private static double baseline(int line) {
        return FIRST_RULE + line * RULE_GAP - 4;
    }

    private static double lineLength(double[] words) {
        double length = (words.length - 1) * WORD_GAP;
        for (double word : words) {
            length += word;
        }
        return length;
    }

    private static <T> KeyValue value(WritableValue<T> target, T end) {
        return new KeyValue(target, end);
    }

    private static Rectangle rectangle(double x, double y, double width, double height, double arc, Paint fill) {
        Rectangle rectangle = new Rectangle(x, y, width, height);
        rectangle.setArcWidth(arc);
        rectangle.setArcHeight(arc);
        rectangle.setFill(fill);
        return rectangle;
    }

    private static Line line(double startX, double startY, double endX, double endY, Color color, double width) {
        Line line = new Line(startX, startY, endX, endY);
        line.setStroke(color);
        line.setStrokeWidth(width);
        line.setStrokeLineCap(StrokeLineCap.ROUND);
        return line;
    }
}

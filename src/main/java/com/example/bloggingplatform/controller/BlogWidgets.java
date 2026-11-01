package com.example.bloggingplatform.controller;

import com.example.bloggingplatform.model.PostStatus;
import com.example.bloggingplatform.model.Tag;
import com.example.bloggingplatform.util.Icons;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/** Small UI pieces shared by the screens: avatars, stars, tag chips, dates, excerpts, form labels and badges. */
final class BlogWidgets {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH);
    private static final int WORDS_PER_MINUTE = 200;

    private BlogWidgets() {
    }

    /** A black circle with the user's initial. */
    static Node avatar(String username, double size) {
        Label letter = new Label(username == null || username.isEmpty() ? "?"
                : username.substring(0, 1).toUpperCase(Locale.ROOT));
        letter.getStyleClass().add("avatar-letter");
        letter.setStyle("-fx-font-size: " + Math.round(size * 0.45) + "px;");
        StackPane circle = new StackPane(letter);
        circle.getStyleClass().add("avatar");
        circle.setMinSize(size, size);
        circle.setPrefSize(size, size);
        circle.setMaxSize(size, size);
        return circle;
    }

    /** "★★★★☆" for 4; text characters, so they follow the black/white text colour. */
    static String stars(int rating) {
        int full = Math.clamp(rating, 0, 5);
        return "★".repeat(full) + "☆".repeat(5 - full);
    }

    /** Stars for an average such as 3.6 (rounded to the nearest star), "☆☆☆☆☆" when there is none. */
    static String stars(Double average) {
        return stars(average == null ? 0 : (int) Math.round(average));
    }

    static Button tagChip(Tag tag, Consumer<Tag> onClick) {
        Button chip = new Button("#" + tag.getName());
        chip.getStyleClass().add("tag-chip");
        chip.setFocusTraversable(false);
        if (onClick == null) {
            chip.setMouseTransparent(true);
        } else {
            chip.setOnAction(e -> onClick.accept(tag));
        }
        return chip;
    }

    static Label statusBadge(PostStatus status) {
        Label badge = new Label(status == PostStatus.PUBLISHED ? "Published" : "Draft");
        badge.getStyleClass().addAll("status-badge", status == PostStatus.PUBLISHED ? "published" : "draft");
        return badge;
    }

    static String date(LocalDateTime time) {
        return time == null ? "" : DATE.format(time);
    }

    /** "3 min read" at about 200 words per minute. */
    static String readingTime(String content) {
        int words = content == null || content.isBlank() ? 0 : content.strip().split("\\s+").length;
        return Math.max(1, (words + WORDS_PER_MINUTE - 1) / WORDS_PER_MINUTE) + " min read";
    }

    /** The start of the text, cut at a word boundary, on one line. */
    static String excerpt(String content, int maxLength) {
        String text = content == null ? "" : content.strip().replaceAll("\\s+", " ");
        if (text.length() <= maxLength) {
            return text;
        }
        int cut = text.lastIndexOf(' ', maxLength);
        return text.substring(0, cut > maxLength / 2 ? cut : maxLength) + "…";
    }

    /** Fills the first {@code count} stars. */
    static void paintStars(List<Button> stars, int count) {
        for (int i = 0; i < stars.size(); i++) {
            stars.get(i).setText(i < count ? "★" : "☆");
        }
    }

    static HBox itemHead(String username, LocalDateTime time, Node extra) {
        Label name = new Label(username);
        name.getStyleClass().add("card-author");
        Label when = new Label(date(time));
        when.getStyleClass().add("card-meta");
        VBox who = new VBox(0, name, when);
        HBox head = new HBox(8, avatar(username, 28), who);
        if (extra != null) {
            head.getChildren().add(extra);
        }
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        head.getChildren().add(spacer);
        head.setAlignment(Pos.CENTER_LEFT);
        return head;
    }

    static Label itemText(String text) {
        Label label = new Label(text);
        label.setWrapText(true);
        label.setMinHeight(Region.USE_PREF_SIZE);
        label.getStyleClass().add("item-text");
        return label;
    }

    static Label badge(String text) {
        Label badge = new Label(text);
        badge.getStyleClass().add("role-badge");
        return badge;
    }

    static Label youBadge() {
        return badge("You");
    }

    static Label formMessage() {
        Label banner = new Label();
        banner.getStyleClass().add("form-message");
        banner.setWrapText(true);
        banner.setMaxWidth(Double.MAX_VALUE);
        return banner;
    }

    static Label fieldLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("field-label");
        return label;
    }

    static Label fieldError() {
        Label error = new Label();
        error.getStyleClass().add("field-error");
        error.setWrapText(true);
        return error;
    }

    static Button iconButton(String icon, String style, String tooltip, Runnable action) {
        Button button = new Button();
        button.setGraphic(Icons.of(icon, 14));
        button.getStyleClass().addAll("btn-icon", style);
        button.setTooltip(new Tooltip(tooltip));
        button.setOnAction(e -> action.run());
        return button;
    }

    static void show(Node node, boolean visible) {
        node.setVisible(visible);
        node.setManaged(visible);
    }
}

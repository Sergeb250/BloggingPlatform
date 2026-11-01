package com.example.bloggingplatform.util;

import javafx.scene.Scene;
import javafx.scene.text.Font;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.lang.System.Logger.Level;
import java.util.List;
import java.util.Objects;

/**
 * The application stylesheet, shared by the main window, pop-ups and the splash screen, and the
 * two bundled fonts (both from Google Fonts, SIL Open Font License, see {@code fonts/*-OFL.txt}):
 * <ul>
 *   <li><b>Xmind</b>, a plain sans-serif, for all text: buttons, menus, labels, forms, posts.</li>
 *   <li><b>Zeyada</b>, a handwriting font, only for a few titles: the app name, page headings,
 *       section headings and pop-up titles.</li>
 * </ul>
 * The fonts are bundled as TTF files instead of being linked from fonts.googleapis.com, because
 * JavaFX cannot read web font links and the app must work offline. They are registered once,
 * before any scene uses them, so CSS can refer to them by family name.
 */
public final class Theme {

    /** Family name of the font for all text in CSS ({@code "Xmind"}; bold is {@code -fx-font-weight: bold}). */
    public static final String TEXT_FONT = "Xmind";
    /** Family name of the handwriting font used for a few titles. */
    public static final String TITLE_FONT = "Zeyada";

    private static final System.Logger LOG = System.getLogger(Theme.class.getName());
    private static final String FONT_FOLDER = "/com/example/bloggingplatform/fonts/";
    private static final List<String> FONT_FILES = List.of("Zeyada-Regular.ttf",
            "Xmind-Regular.ttf", "Xmind-Bold.ttf", "Xmind-SemiBold.ttf", "Xmind-Italic.ttf");
    private static final String STYLESHEET = Objects.requireNonNull(
            Theme.class.getResource("/com/example/bloggingplatform/view/app.css"), "app.css is missing")
            .toExternalForm();

    static {
        FONT_FILES.forEach(Theme::loadFont);
    }

    private Theme() {
    }

    public static void apply(Scene scene) {
        scene.getStylesheets().add(STYLESHEET);
    }

    /** Registers one bundled font with JavaFX; text falls back to the default font if this fails. */
    private static void loadFont(String file) {
        try (InputStream in = Theme.class.getResourceAsStream(FONT_FOLDER + file)) {
            if (in == null || Font.loadFont(in, 12) == null) {
                LOG.log(Level.WARNING, "Could not load font " + file + "; the default font is used instead");
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read font " + file, e);
        }
    }
}

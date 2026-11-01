package com.example.bloggingplatform.util;

import javafx.scene.Node;
import javafx.scene.layout.Pane;
import javafx.scene.shape.FillRule;
import javafx.scene.shape.SVGPath;
import javafx.scene.transform.Scale;

/**
 * Vector icons drawn as {@link SVGPath}s, so they take their colour from CSS ({@code .icon { -fx-fill }})
 * and stay sharp at any size. All paths use a 24 × 24 view box; most are Material Design icons
 * (Apache License 2.0).
 */
public final class Icons {

    // window controls
    public static final String MINIMIZE = "M5 11.4h14v1.4H5Z";
    public static final String MAXIMIZE = "M5 5h14v14H5Zm1.5 1.5v11h11v-11Z";
    public static final String RESTORE = "M8 4h12v12h-3.5v-1.5h2v-9h-9v2H8Zm-4 4h12v12H4Zm1.5 1.5v9h9v-9Z";
    public static final String CLOSE = "M6.4 5 5 6.4 10.6 12 5 17.6 6.4 19 12 13.4 17.6 19 19 17.6 13.4 12 19 6.4 17.6 5 12 10.6Z";

    // sidebar
    public static final String DISCOVER = "M10 20v-6h4v6h5v-8h3L12 3 2 12h3v8z";
    public static final String DASHBOARD = "M3 13h8V3H3v10zm0 8h8v-6H3v6zm10 0h8V11h-8v10zm0-18v6h8V3h-8z";
    public static final String WRITE = "M3 17.25V21h3.75L17.81 9.94l-3.75-3.75L3 17.25zM20.71 7.04c.39-.39.39-1.02 0-1.41"
            + "l-2.34-2.34c-.39-.39-1.02-.39-1.41 0l-1.83 1.83 3.75 3.75 1.83-1.83z";
    public static final String POSTS = "M19 3H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm-5 14H7"
            + "v-2h7v2zm3-4H7v-2h10v2zm0-4H7V7h10v2z";
    public static final String COMMENTS = "M20 2H4c-1.1 0-2 .9-2 2v18l4-4h14c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2z";
    public static final String REVIEWS = "M12 17.27 18.18 21l-1.64-7.03L22 9.24l-7.19-.61L12 2 9.19 8.63 2 9.24l5.46 4.73L5.82 21z";
    public static final String TAGS = "M21.41 11.58l-9-9C12.05 2.22 11.55 2 11 2H4c-1.1 0-2 .9-2 2v7c0 .55.22 1.05.59 1.42l9 9"
            + "c.36.36.86.58 1.41.58.55 0 1.05-.22 1.41-.59l7-7c.37-.36.59-.86.59-1.41 0-.55-.23-1.06-.59-1.42z"
            + "M5.5 7C4.67 7 4 6.33 4 5.5S4.67 4 5.5 4 7 4.67 7 5.5 6.33 7 5.5 7z";
    public static final String USERS = "M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16"
            + "v-2c0-2.66-5.33-4-8-4z";
    public static final String EXIT = "M10.09 15.59 11.5 17l5-5-5-5-1.41 1.41L12.67 11H3v2h9.67l-2.58 2.59zM19 3H5c-1.11 0-2"
            + " .9-2 2v4h2V5h14v14H5v-4H3v4c0 1.1.89 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2z";

    // actions and feedback
    public static final String SEARCH = "M10 2a8 8 0 1 0 4.9 14.3l5.4 5.4 1.4-1.4-5.4-5.4A8 8 0 0 0 10 2Zm0 2a6 6 0 1 1 0 12"
            + " 6 6 0 0 1 0-12Z";
    public static final String ADD = "M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z";
    public static final String EDIT = "M3 17.25V21h3.75L17.81 9.94l-3.75-3.75L3 17.25zM20.71 7.04c.39-.39.39-1.02 0-1.41"
            + "l-2.34-2.34c-.39-.39-1.02-.39-1.41 0l-1.83 1.83 3.75 3.75 1.83-1.83z";
    public static final String DELETE = "M6 19c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7H6v12zM19 4h-3.5l-1-1h-5l-1 1H5v2h14V4z";
    public static final String CHECK = "M9 16.17 4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41z";
    public static final String VIEW = "M12 4.5C7 4.5 2.73 7.61 1 12c1.73 4.39 6 7.5 11 7.5s9.27-3.11 11-7.5c-1.73-4.39-6-7.5"
            + "-11-7.5zM12 17c-2.76 0-5-2.24-5-5s2.24-5 5-5 5 2.24 5 5-2.24 5-5 5zm0-8c-1.66 0-3 1.34-3 3s1.34 3 3 3"
            + " 3-1.34 3-3-1.34-3-3-3z";
    public static final String PUBLISH = "M5 4v2h14V4H5zm0 10h4v6h6v-6h4l-7-7-7 7z";
    public static final String UNPUBLISH = "M20.54 5.23l-1.39-1.68C18.88 3.21 18.47 3 18 3H6c-.47 0-.88.21-1.16.55L3.46 5.23"
            + "C3.17 5.57 3 6.02 3 6.5V19c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V6.5c0-.48-.17-.93-.46-1.27zM12 17.5 6.5 12H10"
            + "v-2h4v2h3.5L12 17.5zM5.12 5l.81-1h12l.94 1H5.12z";
    public static final String BACK = "M20 11H7.83l5.59-5.59L12 4l-8 8 8 8 1.41-1.41L7.83 13H20v-2z";
    public static final String FORWARD = "M12 4l-1.41 1.41L16.17 11H4v2h12.17l-5.58 5.59L12 20l8-8z";
    public static final String SIGN_OUT = "M17 7l-1.41 1.41L18.17 11H8v2h10.17l-2.58 2.58L17 17l5-5zM4 5h8V3H4c-1.1 0-2"
            + " .9-2 2v14c0 1.1.9 2 2 2h8v-2H4V5z";
    public static final String CLOCK = "M12 2C6.5 2 2 6.5 2 12s4.5 10 10 10 10-4.5 10-10S17.5 2 12 2zm0 18c-4.41 0-8-3.59"
            + "-8-8s3.59-8 8-8 8 3.59 8 8-3.59 8-8 8zm.5-13H11v6l5.2 3.2.8-1.3-4.5-2.7V7z";
    public static final String INFO = "M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-6h2v6z"
            + "m0-8h-2V7h2v2z";
    public static final String WARNING = "M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-2h2v2z"
            + "m0-4h-2V7h2v6z";

    private static final double VIEW_BOX = 24;

    private Icons() {
    }

    /** An icon {@code size} pixels square; style it with {@code .icon { -fx-fill: ... }}. */
    public static Node of(String path, double size) {
        SVGPath svg = new SVGPath();
        svg.setContent(path);
        svg.setFillRule(FillRule.EVEN_ODD);
        svg.getStyleClass().add("icon");
        svg.getTransforms().add(new Scale(size / VIEW_BOX, size / VIEW_BOX, 0, 0));
        // a fixed-size box, so icons with different shapes still line up
        Pane box = new Pane(svg);
        box.setMinSize(size, size);
        box.setPrefSize(size, size);
        box.setMaxSize(size, size);
        return box;
    }
}

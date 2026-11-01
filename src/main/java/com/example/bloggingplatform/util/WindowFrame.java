package com.example.bloggingplatform.util;

import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.geometry.Insets;
import javafx.geometry.Rectangle2D;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.util.List;
import java.util.function.BooleanSupplier;

/**
 * Window behaviour for a {@code StageStyle.TRANSPARENT} stage, which gets no title bar or border
 * from the operating system: moving by dragging the title bar, maximize / restore (also by
 * double-clicking the title bar), minimize, and resizing by dragging any edge or corner.
 * <p>
 * The root keeps {@link #SHADOW_INSET} pixels of transparent padding around the visible window
 * for its drop shadow. While maximized the root gets the style class {@code maximized}, which
 * removes the shadow, border and rounded corners, and the padding is removed.
 */
public final class WindowFrame {

    /** Transparent space around the window that holds its shadow. */
    public static final double SHADOW_INSET = 16;
    static final String MAXIMIZED_STYLE = "maximized";
    /** How far into the window an edge can still be grabbed for resizing. */
    private static final double GRIP = 6;

    /** Which edge(s) a resize drag moves. */
    enum Edge {
        NONE(false, false, false, false), N(true, false, false, false), S(false, true, false, false),
        E(false, false, true, false), W(false, false, false, true), NE(true, false, true, false),
        NW(true, false, false, true), SE(false, true, true, false), SW(false, true, false, true);

        final boolean north;
        final boolean south;
        final boolean east;
        final boolean west;

        Edge(boolean north, boolean south, boolean east, boolean west) {
            this.north = north;
            this.south = south;
            this.east = east;
            this.west = west;
        }

        Cursor cursor() {
            return switch (this) {
                case NONE -> null;
                case N -> Cursor.N_RESIZE;
                case S -> Cursor.S_RESIZE;
                case E -> Cursor.E_RESIZE;
                case W -> Cursor.W_RESIZE;
                case NE -> Cursor.NE_RESIZE;
                case NW -> Cursor.NW_RESIZE;
                case SE -> Cursor.SE_RESIZE;
                case SW -> Cursor.SW_RESIZE;
            };
        }
    }

    private final Stage stage;
    private final Region root;
    private final ReadOnlyBooleanWrapper maximized = new ReadOnlyBooleanWrapper(this, "maximized", false);
    private Rectangle2D restoreBounds;

    private double dragOffsetX;
    private double dragOffsetY;

    private WindowFrame(Stage stage, Region root) {
        this.stage = stage;
        this.root = root;
        root.setPadding(new Insets(SHADOW_INSET));
    }

    /**
     * Adds moving (by {@code titleBar}), resizing (from the edges of {@code root}) and maximizing
     * to an undecorated stage whose scene root is {@code root}.
     */
    public static WindowFrame install(Stage stage, Region root, Node titleBar) {
        WindowFrame frame = new WindowFrame(stage, root);
        frame.makeMovable(titleBar);
        enableResizing(stage, root, SHADOW_INSET + GRIP, frame::isMaximized);
        return frame;
    }

    /**
     * Lets the user resize an undecorated stage by dragging any edge or corner of {@code root}
     * ({@code grabZone} pixels wide), never below the stage's minimum size. Event filters run
     * before the window's own controls, so an edge drag never reaches them.
     *
     * @param paused while it returns {@code true} (e.g. maximized) the edges cannot be dragged
     */
    public static void enableResizing(Stage stage, Region root, double grabZone, BooleanSupplier paused) {
        Edge[] resizing = {Edge.NONE};
        double[] start = new double[2];
        Rectangle2D[] startBounds = new Rectangle2D[1];
        root.addEventFilter(MouseEvent.MOUSE_MOVED, e -> root.setCursor(paused.getAsBoolean() ? null
                : edgeAt(e.getSceneX(), e.getSceneY(), root.getWidth(), root.getHeight(), grabZone).cursor()));
        root.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> {
            resizing[0] = paused.getAsBoolean() || e.getButton() != MouseButton.PRIMARY ? Edge.NONE
                    : edgeAt(e.getSceneX(), e.getSceneY(), root.getWidth(), root.getHeight(), grabZone);
            if (resizing[0] != Edge.NONE) {
                start[0] = e.getScreenX();
                start[1] = e.getScreenY();
                startBounds[0] = new Rectangle2D(stage.getX(), stage.getY(), stage.getWidth(), stage.getHeight());
                e.consume();
            }
        });
        root.addEventFilter(MouseEvent.MOUSE_DRAGGED, e -> {
            if (resizing[0] != Edge.NONE) {
                Rectangle2D bounds = resized(startBounds[0], resizing[0], e.getScreenX() - start[0],
                        e.getScreenY() - start[1], stage.getMinWidth(), stage.getMinHeight());
                stage.setX(bounds.getMinX());
                stage.setY(bounds.getMinY());
                stage.setWidth(bounds.getWidth());
                stage.setHeight(bounds.getHeight());
                e.consume();
            }
        });
        root.addEventFilter(MouseEvent.MOUSE_RELEASED, e -> {
            if (resizing[0] != Edge.NONE) {
                resizing[0] = Edge.NONE;
                e.consume();
            }
        });
    }

    /** Lets the user move an undecorated stage, such as a pop-up, by dragging {@code handle}. */
    public static void makeDraggable(Stage stage, Node handle) {
        double[] offset = new double[2];
        handle.setOnMousePressed(e -> {
            offset[0] = stage.getX() - e.getScreenX();
            offset[1] = stage.getY() - e.getScreenY();
        });
        handle.setOnMouseDragged(e -> {
            stage.setX(e.getScreenX() + offset[0]);
            stage.setY(e.getScreenY() + offset[1]);
        });
    }

    /**
     * Gives the window {@code fraction} of the screen's usable area (but at most
     * {@code maxWidth × maxHeight}, plus the shadow) and centres it, so it does not open full screen.
     */
    public void sizeToScreen(double fraction, double maxWidth, double maxHeight) {
        Rectangle2D screen = Screen.getPrimary().getVisualBounds();
        double width = Math.max(stage.getMinWidth(), Math.min(maxWidth, screen.getWidth() * fraction) + 2 * SHADOW_INSET);
        double height = Math.max(stage.getMinHeight(), Math.min(maxHeight, screen.getHeight() * fraction) + 2 * SHADOW_INSET);
        stage.setWidth(Math.min(width, screen.getWidth()));
        stage.setHeight(Math.min(height, screen.getHeight()));
        stage.setX(screen.getMinX() + (screen.getWidth() - stage.getWidth()) / 2);
        stage.setY(screen.getMinY() + (screen.getHeight() - stage.getHeight()) / 2);
    }

    public void minimize() {
        stage.setIconified(true);
    }

    public void toggleMaximize() {
        if (isMaximized()) {
            restore();
        } else {
            maximize();
        }
    }

    public boolean isMaximized() {
        return maximized.get();
    }

    public ReadOnlyBooleanProperty maximizedProperty() {
        return maximized.getReadOnlyProperty();
    }

    /** Which edge a point in the window lies on; {@code zone} is the grab width from each side. */
    static Edge edgeAt(double x, double y, double width, double height, double zone) {
        boolean north = y < zone;
        boolean south = y > height - zone;
        boolean west = x < zone;
        boolean east = x > width - zone;
        if (north && west) {
            return Edge.NW;
        } else if (north && east) {
            return Edge.NE;
        } else if (south && west) {
            return Edge.SW;
        } else if (south && east) {
            return Edge.SE;
        } else if (north) {
            return Edge.N;
        } else if (south) {
            return Edge.S;
        } else if (west) {
            return Edge.W;
        }
        return east ? Edge.E : Edge.NONE;
    }

    /**
     * Stage size after dragging {@code edge} by ({@code dx}, {@code dy}) from {@code start}, never
     * smaller than the minimum size. The opposite edge stays where it is.
     */
    static Rectangle2D resized(Rectangle2D start, Edge edge, double dx, double dy, double minWidth, double minHeight) {
        double x = start.getMinX();
        double y = start.getMinY();
        double width = start.getWidth();
        double height = start.getHeight();
        if (edge.east) {
            width = Math.max(minWidth, width + dx);
        }
        if (edge.south) {
            height = Math.max(minHeight, height + dy);
        }
        if (edge.west) {
            double newWidth = Math.max(minWidth, width - dx);
            x += width - newWidth;
            width = newWidth;
        }
        if (edge.north) {
            double newHeight = Math.max(minHeight, height - dy);
            y += height - newHeight;
            height = newHeight;
        }
        return new Rectangle2D(x, y, width, height);
    }

    private void makeMovable(Node titleBar) {
        titleBar.setOnMousePressed(e -> {
            dragOffsetX = stage.getX() - e.getScreenX();
            dragOffsetY = stage.getY() - e.getScreenY();
        });
        titleBar.setOnMouseDragged(e -> {
            if (e.getButton() != MouseButton.PRIMARY) {
                return;   // edge resizes never get here: their events are consumed by enableResizing
            }
            if (isMaximized()) {
                // dragging a maximized window restores it under the cursor, at the same relative spot
                double ratio = (e.getScreenX() - stage.getX()) / stage.getWidth();
                double titleBarY = e.getSceneY();
                restore();
                dragOffsetX = -stage.getWidth() * ratio;
                dragOffsetY = -(SHADOW_INSET + titleBarY);
            }
            stage.setX(e.getScreenX() + dragOffsetX);
            stage.setY(e.getScreenY() + dragOffsetY);
        });
        titleBar.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2) {
                toggleMaximize();
            }
        });
    }

    private void maximize() {
        restoreBounds = bounds();
        root.setPadding(Insets.EMPTY);
        root.getStyleClass().add(MAXIMIZED_STYLE);
        setBounds(screenOf(restoreBounds).getVisualBounds());   // the usable area, so the taskbar stays visible
        maximized.set(true);
    }

    private void restore() {
        root.setPadding(new Insets(SHADOW_INSET));
        root.getStyleClass().remove(MAXIMIZED_STYLE);
        setBounds(restoreBounds);
        maximized.set(false);
    }

    private Rectangle2D bounds() {
        return new Rectangle2D(stage.getX(), stage.getY(), stage.getWidth(), stage.getHeight());
    }

    private void setBounds(Rectangle2D bounds) {
        stage.setX(bounds.getMinX());
        stage.setY(bounds.getMinY());
        stage.setWidth(bounds.getWidth());
        stage.setHeight(bounds.getHeight());
    }

    private static Screen screenOf(Rectangle2D bounds) {
        List<Screen> screens = Screen.getScreensForRectangle(bounds);
        return screens.isEmpty() ? Screen.getPrimary() : screens.getFirst();
    }
}

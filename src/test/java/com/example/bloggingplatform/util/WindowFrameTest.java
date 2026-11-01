package com.example.bloggingplatform.util;

import javafx.geometry.Rectangle2D;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WindowFrameTest {

    private static final double ZONE = 10;

    @Test
    void edgesAndCornersAreFoundNearTheBorder() {
        assertEquals(WindowFrame.Edge.NW, WindowFrame.edgeAt(2, 2, 800, 600, ZONE));
        assertEquals(WindowFrame.Edge.SE, WindowFrame.edgeAt(795, 595, 800, 600, ZONE));
        assertEquals(WindowFrame.Edge.N, WindowFrame.edgeAt(400, 5, 800, 600, ZONE));
        assertEquals(WindowFrame.Edge.W, WindowFrame.edgeAt(3, 300, 800, 600, ZONE));
        assertEquals(WindowFrame.Edge.NONE, WindowFrame.edgeAt(400, 300, 800, 600, ZONE));
    }

    @Test
    void draggingTheLeftEdgeKeepsTheRightEdgeInPlace() {
        Rectangle2D start = new Rectangle2D(100, 100, 800, 600);

        Rectangle2D wider = WindowFrame.resized(start, WindowFrame.Edge.W, -50, 0, 400, 300);
        assertEquals(new Rectangle2D(50, 100, 850, 600), wider);

        Rectangle2D tooNarrow = WindowFrame.resized(start, WindowFrame.Edge.W, 700, 0, 400, 300);
        assertEquals(400, tooNarrow.getWidth());
        assertEquals(start.getMaxX(), tooNarrow.getMaxX());
    }

    @Test
    void cornerDragResizesBothDirectionsButNotBelowTheMinimum() {
        Rectangle2D start = new Rectangle2D(0, 0, 800, 600);

        assertEquals(new Rectangle2D(0, 0, 900, 650), WindowFrame.resized(start, WindowFrame.Edge.SE, 100, 50, 400, 300));
        assertEquals(new Rectangle2D(0, 0, 400, 300), WindowFrame.resized(start, WindowFrame.Edge.SE, -900, -900, 400, 300));
    }
}

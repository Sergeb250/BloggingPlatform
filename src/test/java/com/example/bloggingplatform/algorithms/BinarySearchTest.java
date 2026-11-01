package com.example.bloggingplatform.algorithms;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BinarySearchTest {

    private final List<String> words = List.of("api", "java", "javafx", "mysql");

    @Test
    void anEmptyListGivesPositionZero() {
        assertEquals(0, BinarySearch.firstAtLeast(List.of(), "java"));
    }

    @Test
    void anExactWordGivesItsPosition() {
        assertEquals(0, BinarySearch.firstAtLeast(words, "api"));
        assertEquals(1, BinarySearch.firstAtLeast(words, "java"));
        assertEquals(3, BinarySearch.firstAtLeast(words, "mysql"));
    }

    @Test
    void aPrefixGivesTheFirstWordStartingWithIt() {
        assertEquals(1, BinarySearch.firstAtLeast(words, "jav"));
        assertEquals(3, BinarySearch.firstAtLeast(words, "my"));
    }

    @Test
    void aMissingWordGivesWhereItWouldBe() {
        assertEquals(0, BinarySearch.firstAtLeast(words, "aaa"));     // before every word
        assertEquals(2, BinarySearch.firstAtLeast(words, "javac"));   // between java and javafx
        assertEquals(4, BinarySearch.firstAtLeast(words, "zoo"));     // after every word
    }

    @Test
    void agreesWithALinearScanForEveryKey() {
        List<String> sorted = List.of("b", "d", "d", "f", "h");
        for (String key : List.of("a", "b", "c", "d", "e", "f", "g", "h", "i")) {
            int expected = 0;
            while (expected < sorted.size() && sorted.get(expected).compareTo(key) < 0) {
                expected++;
            }
            assertEquals(expected, BinarySearch.firstAtLeast(sorted, key), key);
        }
    }
}

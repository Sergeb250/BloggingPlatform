package com.example.bloggingplatform.algorithms;

import java.util.List;

/**
 * Binary search on a list sorted A-Z: look at the middle item and throw away the half that cannot
 * hold the answer, until one position is left. O(log n): 10,000 words take at most 14 steps.
 */
public final class BinarySearch {

    private BinarySearch() {
    }

    /**
     * The position of the first word that is equal to or after {@code key} in A-Z order, or
     * {@code sorted.size()} when every word comes before it. The list must be sorted A-Z.
     * <p>
     * Example: in [api, java, javafx, mysql], "jav" gives 1 (java) and "zoo" gives 4.
     */
    public static int firstAtLeast(List<String> sorted, String key) {
        int low = 0;
        int high = sorted.size();   // the answer is always between low and high
        while (low < high) {
            int middle = (low + high) / 2;
            if (sorted.get(middle).compareTo(key) < 0) {
                low = middle + 1;   // the middle word is too small: the answer is to its right
            } else {
                high = middle;      // the middle word is big enough: the answer is here or to its left
            }
        }
        return low;
    }
}

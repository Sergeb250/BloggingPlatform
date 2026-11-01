package com.example.bloggingplatform.algorithms;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Merge sort: split the list in two halves, sort each half, then merge the two sorted halves.
 * <p>
 * Always O(n log n) time and O(n) extra memory. It is stable: items that compare as equal keep
 * their order. Used to sort Discover's posts and the words of the search index.
 */
public final class MergeSort {

    private MergeSort() {
    }

    /** Sorts {@code list} in place, smallest first according to {@code order}. */
    public static <T> void sort(List<T> list, Comparator<? super T> order) {
        if (list.size() < 2) {
            return;   // 0 or 1 item is already sorted
        }
        int middle = list.size() / 2;
        List<T> left = new ArrayList<>(list.subList(0, middle));
        List<T> right = new ArrayList<>(list.subList(middle, list.size()));
        sort(left, order);
        sort(right, order);
        merge(left, right, list, order);
    }

    /** Copies the two sorted halves back into {@code target}, always taking the smaller front item. */
    private static <T> void merge(List<T> left, List<T> right, List<T> target, Comparator<? super T> order) {
        int l = 0;
        int r = 0;
        int t = 0;
        while (l < left.size() && r < right.size()) {
            if (order.compare(left.get(l), right.get(r)) <= 0) {   // <= keeps equal items in order (stable)
                target.set(t, left.get(l));
                l++;
            } else {
                target.set(t, right.get(r));
                r++;
            }
            t++;
        }
        while (l < left.size()) {   // whatever is left over is already sorted
            target.set(t, left.get(l));
            l++;
            t++;
        }
        while (r < right.size()) {
            target.set(t, right.get(r));
            r++;
            t++;
        }
    }
}

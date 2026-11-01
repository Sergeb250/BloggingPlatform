package com.example.bloggingplatform.algorithms;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MergeSortTest {

    @Test
    void emptyAndOneItemListsStayAsTheyAre() {
        List<Integer> empty = new ArrayList<>();
        List<Integer> one = new ArrayList<>(List.of(7));

        MergeSort.sort(empty, Comparator.naturalOrder());
        MergeSort.sort(one, Comparator.naturalOrder());

        assertEquals(List.of(), empty);
        assertEquals(List.of(7), one);
    }

    @Test
    void sortsUnsortedReversedAndAlreadySortedLists() {
        List<Integer> unsorted = new ArrayList<>(List.of(5, 1, 4, 2, 3));
        List<Integer> reversed = new ArrayList<>(List.of(5, 4, 3, 2, 1));
        List<Integer> sorted = new ArrayList<>(List.of(1, 2, 3, 4, 5));

        MergeSort.sort(unsorted, Comparator.naturalOrder());
        MergeSort.sort(reversed, Comparator.naturalOrder());
        MergeSort.sort(sorted, Comparator.naturalOrder());

        assertEquals(List.of(1, 2, 3, 4, 5), unsorted);
        assertEquals(List.of(1, 2, 3, 4, 5), reversed);
        assertEquals(List.of(1, 2, 3, 4, 5), sorted);
    }

    @Test
    void theComparatorDecidesTheOrder() {
        List<String> words = new ArrayList<>(List.of("bb", "a", "ccc"));

        MergeSort.sort(words, Comparator.comparing(String::length).reversed());

        assertEquals(List.of("ccc", "bb", "a"), words);
    }

    @Test
    void equalItemsKeepTheirOrder() {
        // sorted by the first letter only: "b1" and "b2" are equal and must stay in this order
        List<String> items = new ArrayList<>(List.of("b1", "a1", "b2", "a2"));

        MergeSort.sort(items, Comparator.comparing(item -> item.charAt(0)));

        assertEquals(List.of("a1", "a2", "b1", "b2"), items);
    }

    @Test
    void givesTheSameResultAsJavasOwnSortOnRandomLists() {
        Random random = new Random(42);
        for (int size = 0; size < 200; size++) {
            List<Integer> mine = new ArrayList<>();
            for (int i = 0; i < size; i++) {
                mine.add(random.nextInt(50));   // small range, so there are many duplicates
            }
            List<Integer> expected = new ArrayList<>(mine);
            expected.sort(Comparator.naturalOrder());

            MergeSort.sort(mine, Comparator.naturalOrder());

            assertEquals(expected, mine, "size " + size);
        }
    }
}

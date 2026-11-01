package com.example.bloggingplatform.cache;

import com.example.bloggingplatform.algorithms.BinarySearch;
import com.example.bloggingplatform.algorithms.MergeSort;
import com.example.bloggingplatform.model.Post;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * The search bar's index (an "inverted index", like the index at the back of a book): every word
 * of a post's title, content, tags and author name points to the ids of the posts that contain it.
 * <p>
 * Words are stored in lower case, so searching ignores case. They are also kept in a sorted list,
 * so binary search can find words that start with what was typed: "jav" finds "java" and "javafx".
 */
public class SearchIndex {

    private final Map<String, Set<Integer>> postsByWord = new HashMap<>();   // word → post ids
    private final List<String> sortedWords;                                    // the same words, A-Z

    public SearchIndex(List<Post> posts) {
        for (Post post : posts) {
            for (String word : words(textOf(post))) {
                if (!postsByWord.containsKey(word)) {
                    postsByWord.put(word, new HashSet<>());
                }
                postsByWord.get(word).add(post.getId());
            }
        }
        sortedWords = new ArrayList<>(postsByWord.keySet());
        MergeSort.sort(sortedWords, Comparator.naturalOrder());
    }

    /**
     * The ids of the posts that contain every word of the query, each as a whole word or as the
     * start of one. Case does not matter. A query with no letters or digits (e.g. "!!") finds nothing.
     */
    public Set<Integer> search(String query) {
        List<String> queryWords = words(query);
        Set<Integer> result = new HashSet<>();
        for (int i = 0; i < queryWords.size(); i++) {
            Set<Integer> ids = postsWithWordStartingWith(queryWords.get(i));
            if (i == 0) {
                result.addAll(ids);
            } else {
                result.retainAll(ids);   // keep only posts that also have this word
            }
        }
        return result;
    }

    /** Binary search finds the first word that could start with {@code prefix}; the others follow it. */
    private Set<Integer> postsWithWordStartingWith(String prefix) {
        Set<Integer> ids = new HashSet<>();
        int i = BinarySearch.firstAtLeast(sortedWords, prefix);
        while (i < sortedWords.size() && sortedWords.get(i).startsWith(prefix)) {
            ids.addAll(postsByWord.get(sortedWords.get(i)));   // hash map lookup: O(1)
            i++;
        }
        return ids;
    }

    /** The lower-case words of a text: runs of letters and digits ("Spring-Boot 3!" → spring, boot, 3). */
    static List<String> words(String text) {
        List<String> words = new ArrayList<>();
        for (String word : text.toLowerCase(Locale.ROOT).split("[^\\p{L}\\p{N}]+")) {
            if (!word.isEmpty()) {
                words.add(word);
            }
        }
        return words;
    }

    /** Everything a post can be found by: title, content, tags and author name. */
    private static String textOf(Post post) {
        String author = post.getAuthorName() == null ? "" : post.getAuthorName();
        return post.getTitle() + " " + post.getContent() + " " + post.getTagNames() + " " + author;
    }
}

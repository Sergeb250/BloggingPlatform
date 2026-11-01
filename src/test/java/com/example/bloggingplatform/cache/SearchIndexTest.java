package com.example.bloggingplatform.cache;

import com.example.bloggingplatform.model.Post;
import com.example.bloggingplatform.model.PostStatus;
import com.example.bloggingplatform.model.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SearchIndexTest {

    private final SearchIndex index = new SearchIndex(List.of(
            post(1, "Getting started with Java", "Classes and objects.", "alice", "java"),
            post(2, "JavaFX layouts", "VBox, HBox and GridPane.", "bob", "javafx"),
            post(3, "MySQL indexes", "A B-tree index makes lookups fast in Java apps.", "carol", "mysql", "design-patterns"),
            post(4, "Café notes", "Writing about coffee.", "dave")));

    @Test
    void searchIgnoresCase() {
        assertEquals(Set.of(3), index.search("mysql"));
        assertEquals(Set.of(3), index.search("MYSQL"));
        assertEquals(Set.of(3), index.search("MySql"));
    }

    @Test
    void aWordCanBeTypedPartly() {
        assertEquals(Set.of(1, 2, 3), index.search("jav"));    // java and javafx
        assertEquals(Set.of(1, 2, 3), index.search("java"));   // "java" is also the start of "javafx"
        assertEquals(Set.of(2), index.search("javaf"));
        assertEquals(Set.of(3), index.search("ind"));          // indexes, index
    }

    @Test
    void everyWordOfTheQueryMustMatch() {
        assertEquals(Set.of(3), index.search("java mysql"));
        assertEquals(Set.of(), index.search("javafx mysql"));
    }

    @Test
    void postsAreFoundByTagAndByAuthor() {
        assertEquals(Set.of(3), index.search("design-patterns"));
        assertEquals(Set.of(3), index.search("Carol"));
        assertEquals(Set.of(2), index.search("BOB"));
    }

    @Test
    void punctuationSplitsWordsAndAccentsWork() {
        assertEquals(Set.of(3), index.search("b-tree"));
        assertEquals(Set.of(2), index.search("gridpane,"));
        assertEquals(Set.of(4), index.search("CAFÉ"));
    }

    @Test
    void queriesWithoutAMatchingWordFindNothing() {
        assertEquals(Set.of(), index.search("python"));
        assertEquals(Set.of(), index.search("!!"));   // no letters or digits at all
        assertEquals(Set.of(), index.search(""));
    }

    @Test
    void anEmptyIndexFindsNothing() {
        assertEquals(Set.of(), new SearchIndex(List.of()).search("java"));
    }

    @Test
    void wordsAreLowerCaseRunsOfLettersAndDigits() {
        assertEquals(List.of("spring", "boot", "3"), SearchIndex.words("Spring-Boot 3!"));
        assertEquals(List.of(), SearchIndex.words("  ... "));
    }

    private static Post post(int id, String title, String content, String author, String... tags) {
        List<Tag> tagList = new java.util.ArrayList<>();
        for (String tag : tags) {
            tagList.add(new Tag(id * 10 + tagList.size(), tag));
        }
        Post post = new Post(id, 1, title, content, PostStatus.PUBLISHED, tagList);
        post.setAuthorName(author);
        return post;
    }
}

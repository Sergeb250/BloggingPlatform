package com.example.bloggingplatform.model;

import java.util.List;

/** One page of a longer list, e.g. posts 7-12 of 15. */
public class Page<T> {

    private final List<T> items;
    private final int pageIndex;       // 0 for the first page
    private final int pageSize;
    private final long totalItems;     // rows matching the query across all pages

    public Page(List<T> items, int pageIndex, int pageSize, long totalItems) {
        this.items = List.copyOf(items);
        this.pageIndex = pageIndex;
        this.pageSize = pageSize;
        this.totalItems = totalItems;
    }

    public List<T> getItems() {
        return items;
    }

    public int getPageIndex() {
        return pageIndex;
    }

    public int getPageSize() {
        return pageSize;
    }

    public long getTotalItems() {
        return totalItems;
    }

    /** Number of pages; at least 1, so an empty result still shows one (empty) page. */
    public int getPageCount() {
        return (int) Math.max(1, (totalItems + pageSize - 1) / pageSize);
    }

    /** Number of the first item on this page, counting from 1; 0 when the page is empty. */
    public long getFirstItemNumber() {
        return items.isEmpty() ? 0 : (long) pageIndex * pageSize + 1;
    }

    /** Number of the last item on this page, counting from 1; 0 when the page is empty. */
    public long getLastItemNumber() {
        return items.isEmpty() ? 0 : (long) pageIndex * pageSize + items.size();
    }
}

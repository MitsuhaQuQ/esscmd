package com.mtmrails.esscmd.util;

import java.util.List;

/** Splits a list into 1-based pages. */
public final class Pager {
    private Pager() {}

    /**
     * @param page the requested page, clamped to the valid range
     * @param pages total pages, at least 1 even for an empty list
     */
    public record Page<T>(List<T> items, int page, int pages) {
        public boolean hasNext() {
            return page < pages;
        }
    }

    public static <T> Page<T> page(List<T> all, int page, int perPage) {
        if (perPage <= 0) {
            throw new IllegalArgumentException("perPage must be positive");
        }
        int pages = Math.max(1, (all.size() + perPage - 1) / perPage);
        int current = Math.min(Math.max(page, 1), pages);
        int from = (current - 1) * perPage;
        int to = Math.min(all.size(), from + perPage);
        return new Page<>(List.copyOf(all.subList(from, to)), current, pages);
    }
}

package com.mtmrails.esscmd.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class PagerTest {
    private static final List<Integer> SEVEN = List.of(1, 2, 3, 4, 5, 6, 7);

    @Test
    void splitsIntoPages() {
        Pager.Page<Integer> first = Pager.page(SEVEN, 1, 3);
        assertEquals(List.of(1, 2, 3), first.items());
        assertEquals(3, first.pages());
        assertTrue(first.hasNext());
        Pager.Page<Integer> last = Pager.page(SEVEN, 3, 3);
        assertEquals(List.of(7), last.items());
        assertFalse(last.hasNext());
    }

    @Test
    void clampsOutOfRangePages() {
        assertEquals(1, Pager.page(SEVEN, 0, 3).page());
        assertEquals(3, Pager.page(SEVEN, 99, 3).page());
    }

    @Test
    void emptyListHasOneEmptyPage() {
        Pager.Page<Integer> page = Pager.page(List.of(), 5, 10);
        assertEquals(1, page.page());
        assertEquals(1, page.pages());
        assertTrue(page.items().isEmpty());
    }

    @Test
    void rejectsNonPositivePageSize() {
        assertThrows(IllegalArgumentException.class, () -> Pager.page(SEVEN, 1, 0));
    }
}

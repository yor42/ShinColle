package com.lulan.shincolle.utility;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Issue #27: "show all" (page <= 0) is capped; pages > 0 are unchanged. */
class ShipListPagingTest {

    private static int[] paging(int size, int page, int perPage) {
        return CommandHelper.resolveShipListPaging(size, page, perPage);
    }

    @Test
    void showAll_isCappedAtDocumentedLimit() {
        assertEquals(100, CommandHelper.SHIP_LIST_SHOW_ALL_MAX);
        for (int page : new int[]{0, -1, Integer.MIN_VALUE}) {
            int[] p = paging(250, page, 10);
            assertEquals(CommandHelper.SHIP_LIST_SHOW_ALL_MAX, p[0], "ships per reply");
            assertEquals(3, p[1], "page count tells the player the list was truncated");
            assertEquals(1, p[2], "page sent");
        }
    }

    @Test
    void showAll_smallWorld_singlePage() {
        int[] p = paging(40, 0, 10);
        assertArrayEquals(new int[]{100, 1, 1}, p);
        assertArrayEquals(new int[]{100, 1, 1}, paging(1, 0, 10));
    }

    @Test
    void positivePage_usesConfigPerPage_unchanged() {
        assertArrayEquals(new int[]{10, 3, 2}, paging(25, 2, 10));
        assertArrayEquals(new int[]{10, 3, 3}, paging(25, 3, 10));
        assertArrayEquals(new int[]{10, 3, 9}, paging(25, 9, 10)); // out of range: caller writes an empty page
        assertArrayEquals(new int[]{10, 1, 1}, paging(10, 1, 10));
    }
}

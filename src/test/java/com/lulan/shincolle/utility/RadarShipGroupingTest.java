package com.lulan.shincolle.utility;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Issue #53: radar list grouping. A ship id goes to its owner's list only when the owner is online and in
 * the same dimension; offline owners have no list; insertion order of ships is preserved.
 */
class RadarShipGroupingTest {

    @Test
    void groupsByOnlineOwnerAndDimension() {
        Map<Integer, Integer> online = new HashMap<>();
        online.put(1, 0);   // player 1 in dim 0
        online.put(2, -1);  // player 2 in the nether
        Map<Integer, List<Integer>> lists = RadarShipGrouping.createLists(online);

        assertTrue(RadarShipGrouping.addShip(lists, online, 100, 1, 0));
        assertFalse(RadarShipGrouping.addShip(lists, online, 101, 1, -1));  // other dimension
        assertTrue(RadarShipGrouping.addShip(lists, online, 102, 2, -1));
        assertFalse(RadarShipGrouping.addShip(lists, online, 103, 3, 0));   // owner offline
        assertTrue(RadarShipGrouping.addShip(lists, online, 104, 1, 0));
        assertFalse(RadarShipGrouping.addShip(lists, online, 105, 0, 0));   // no owner

        assertEquals(Arrays.asList(100, 104), lists.get(1));
        assertEquals(Arrays.asList(102), lists.get(2));
        assertEquals(2, lists.size());
        assertFalse(lists.containsKey(3));
    }

    @Test
    void onlinePlayerWithoutShipsGetsEmptyList() {
        Map<Integer, Integer> online = new HashMap<>();
        online.put(5, 0);
        Map<Integer, List<Integer>> lists = RadarShipGrouping.createLists(online);
        assertTrue(lists.get(5).isEmpty());
    }
}

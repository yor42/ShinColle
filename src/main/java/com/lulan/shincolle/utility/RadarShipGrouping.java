package com.lulan.shincolle.utility;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * (#53) Minecraft-independent grouping rule of the radar ship list sent by ServerProxy.updateServerTick.
 * A ship id is listed for its owner only when the owner is online and in the same dimension as the ship.
 */
public final class RadarShipGrouping {

    private RadarShipGrouping() {
    }

    /**
     * create the empty list for each ONLINE player uid; offline players never receive a list
     *
     * @param onlineDimByPid online player uid -> dimension id of the player
     */
    public static Map<Integer, List<Integer>> createLists(Map<Integer, Integer> onlineDimByPid) {
        Map<Integer, List<Integer>> lists = new HashMap<Integer, List<Integer>>();

        for (Integer pid : onlineDimByPid.keySet()) {
            lists.put(pid, new java.util.ArrayList<Integer>());
        }

        return lists;
    }

    /**
     * add the ship entity id to its owner's list if the owner is online and in the ship's dimension
     *
     * @return true if added
     */
    public static boolean addShip(Map<Integer, List<Integer>> lists, Map<Integer, Integer> onlineDimByPid,
                                  int shipEntityId, int ownerPid, int shipDim) {
        Integer ownerDim = onlineDimByPid.get(ownerPid);

        if (ownerDim != null && ownerDim == shipDim) {
            List<Integer> list = lists.get(ownerPid);

            if (list != null) {
                list.add(shipEntityId);
                return true;
            }
        }

        return false;
    }
}

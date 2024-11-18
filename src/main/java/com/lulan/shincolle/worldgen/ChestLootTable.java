package com.lulan.shincolle.worldgen;

import com.lulan.shincolle.Tags;
import com.lulan.shincolle.config.ConfigLoot;
import com.lulan.shincolle.config.ConfigLoot.ItemEntry;
import com.lulan.shincolle.item.BasicItem;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.storage.loot.*;
import net.minecraft.world.storage.loot.conditions.LootCondition;
import net.minecraft.world.storage.loot.conditions.RandomChance;
import net.minecraft.world.storage.loot.functions.LootFunction;
import net.minecraft.world.storage.loot.functions.SetCount;
import net.minecraft.world.storage.loot.functions.SetMetadata;
import net.minecraftforge.event.LootTableLoadEvent;

import java.util.*;

/**
 * custom loot table:
 * loot table只能在LootTableLoadEvent中修改, 之後就會設為為final而不能再修改
 * 任何再event以外的地方修改table都會跳exception
 * <p>
 * loot pool:
 * vanilla的loot table除非為空, 不然必有"main"這個pool, 若有多個pool則會取名為 "pool1" "pool2" "pool3" ...
 * 詳見ForgeHooks::readPoolName
 */
public class ChestLootTable {

    private static int LootCount = 0;

    private static final Map<ResourceLocation, Integer> lootTableMappings = new HashMap<ResourceLocation, Integer>() {{
        put(LootTableList.CHESTS_SPAWN_BONUS_CHEST, 0);
        put(LootTableList.CHESTS_IGLOO_CHEST, 1);
        put(LootTableList.CHESTS_SIMPLE_DUNGEON, 2);
        put(LootTableList.CHESTS_VILLAGE_BLACKSMITH, 3);
        put(LootTableList.CHESTS_ABANDONED_MINESHAFT, 4);
        put(LootTableList.CHESTS_DESERT_PYRAMID, 5);
        put(LootTableList.CHESTS_JUNGLE_TEMPLE, 6);
        put(LootTableList.CHESTS_NETHER_BRIDGE, 7);
        put(LootTableList.CHESTS_END_CITY_TREASURE, 9);
    }};

    private static final  Set<ResourceLocation> strongholdChests = new HashSet<>(Arrays.asList(
            LootTableList.CHESTS_STRONGHOLD_LIBRARY,
            LootTableList.CHESTS_STRONGHOLD_CROSSING,
            LootTableList.CHESTS_STRONGHOLD_CORRIDOR
    ));

    public ChestLootTable() {
    }

    /**
     * chest_ID:
     * 0:Spawn Bonus Chest, 1:Igloo, 2:Dungeon, 3:Village Blacksmith, 4:Mineshaft, 5:Pyramid
     * 6:Jungle Temple, 7:Nether Bridge, 8:Stronghold, 9:End City"+NEW_LINE);
     */
    public static void editLoot(LootTableLoadEvent event) {
        final ResourceLocation host = event.getName();
        final LootTable table = event.getTable();

        if (strongholdChests.contains(host)) {
            addNewPoolToTable(table, 8);
            return;
        }

        Integer poolId = lootTableMappings.get(host);
        if (poolId != null) {
            addNewPoolToTable(table, poolId);
        }
    }

    private static LootEntryItem convItemEntryToLootEntry(ItemEntry ent) {
        if (ent == null) {
            return null;
        }
        //init entry value
        //set item
        Item item = Item.getByNameOrId(ent.itemName);

        //set function
        ArrayList<LootFunction> funcList = new ArrayList<LootFunction>();

        //add function: meta value
        //specific meta
        if (ent.itemMeta > 0) {
            funcList.add(new SetMetadata(new LootCondition[0], new RandomValueRange(ent.itemMeta)));
        }
        //random meta, only for BasicItem
        else if (ent.itemMeta == -1) {
            if (item instanceof BasicItem) {
                int maxMeta = ((BasicItem) item).getTypes() - 1;
                funcList.add(new SetMetadata(new LootCondition[0], new RandomValueRange(0, maxMeta)));
            }
        }

        //add function: count
        funcList.add(new SetCount(new LootCondition[0], new RandomValueRange(ent.min, ent.max)));

        //set condition
        LootCondition[] condList = new LootCondition[]{new RandomChance(ent.chance)};

        //set entry name
        String entryName = Tags.MOD_ID + ":lootentry" + LootCount;
        LootCount++;

        //return entry
        if (item == null) {
            return null;
        }
        return new LootEntryItem(item, ent.weight, 0, funcList.toArray(new LootFunction[0]), condList, entryName);

    }

    private static void addNewPoolToTable(LootTable table, int chestID) {
        //conv item entry to loot entry
        ArrayList<LootEntry> lootList = new ArrayList<LootEntry>();
        ArrayList<ItemEntry> lootlist2 = ConfigLoot.LOOTMAP.get(chestID);
        String poolName = Tags.MOD_ID + "Pool";

        for (ItemEntry ent : lootlist2) {
            LootEntryItem ent2 = convItemEntryToLootEntry(ent);
            if (ent2 != null) {
                lootList.add(ent2);
            }
        }

        //add pool
        if (!lootList.isEmpty()) {
            table.addPool(new LootPool(lootList.toArray(new LootEntry[lootList.size()]),
                    //always can roll
                    new LootCondition[0],
                    //roll 1 ~ N/2 + 1 times
                    new RandomValueRange(1, lootList.size() / 2 + 1),
                    //bonus roll +1 per luck level
                    new RandomValueRange(1),
                    poolName));
        }
    }


}
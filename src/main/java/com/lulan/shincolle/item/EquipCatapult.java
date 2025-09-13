package com.lulan.shincolle.item;

import com.lulan.shincolle.reference.ID;
import net.minecraft.item.ItemStack;

/**
 * meta:
 * 0:  Catapult Type F MkII
 * 1:  Catapult Type H
 * 2:  Catapult Type C
 * 3:  Electromagnetic Catapult
 */
public class EquipCatapult extends BasicEquip {

    private static final String NAME = "EquipCatapult";


    public EquipCatapult() {
        super();
        this.setTranslationKey(NAME);
        this.setHasSubtypes(true);
    }

    @Override
    public int getTypes() {
        return 4;
    }

    @Override
    public int getEquipTypeIDFromMeta(int meta) {
        return switch (meta) {
            case 0, 1 -> ID.EquipType.CATAPULT_LO;
            case 2, 3 -> ID.EquipType.CATAPULT_HI;
            default -> 0;
        };
    }

    @Override
    public int getItemEnchantability(ItemStack stack) {
        return switch (this.getEquipTypeIDFromMeta(stack.getMetadata())) {
            case ID.EquipType.CATAPULT_LO -> 18;
            case ID.EquipType.CATAPULT_HI -> 25;
            default -> 9;
        };
    }

    @Override
    public int[] getResourceValue(int meta) {
        return switch (this.getEquipTypeIDFromMeta(meta)) {
            case ID.EquipType.CATAPULT_LO ->  //2800
                    new int[]{itemRand.nextInt(40) + 120,
                            itemRand.nextInt(50) + 150,
                            itemRand.nextInt(30) + 80,
                            itemRand.nextInt(60) + 180};
            case ID.EquipType.CATAPULT_HI ->  //5000
                    new int[]{itemRand.nextInt(70) + 190,
                            itemRand.nextInt(85) + 230,
                            itemRand.nextInt(55) + 150,
                            itemRand.nextInt(90) + 250};
            default -> new int[]{0, 0, 0, 0};
        };
    }


}


package com.lulan.shincolle.item;

import com.lulan.shincolle.reference.ID;
import net.minecraft.item.ItemStack;

/**
 * meta:
 * 0:  Abyssal Boiler
 * 1:  Improved Abyssal Turbine
 * 2:  Enhanced Abyssal Boiler
 * 3:  Abyssal Grudge Engine
 * 4:  New Model Abyssal Grudge Engine
 */
public class EquipTurbine extends BasicEquip {

    private static final String NAME = "EquipTurbine";


    public EquipTurbine() {
        super();
        this.setTranslationKey(NAME);
        this.setHasSubtypes(true);
    }

    @Override
    public int getTypes() {
        return 5;
    }

    @Override
    public int getEquipTypeIDFromMeta(int meta) {
        return switch (meta) {
            case 0, 1 -> ID.EquipType.TURBINE_LO;
            case 2, 3, 4 -> ID.EquipType.TURBINE_HI;
            default -> 0;
        };
    }

    @Override
    public int getItemEnchantability(ItemStack stack) {
        return switch (this.getEquipTypeIDFromMeta(stack.getMetadata())) {
            case ID.EquipType.TURBINE_LO -> 18;
            case ID.EquipType.TURBINE_HI -> 25;
            default -> 9;
        };
    }

    @Override
    public int[] getResourceValue(int meta) {
        return switch (this.getEquipTypeIDFromMeta(meta)) {
            case ID.EquipType.TURBINE_LO ->  //1400
                    new int[]{itemRand.nextInt(35) + 90,
                            itemRand.nextInt(25) + 80,
                            itemRand.nextInt(15) + 45,
                            itemRand.nextInt(20) + 60};
            case ID.EquipType.TURBINE_HI ->  //3200
                    new int[]{itemRand.nextInt(70) + 200,
                            itemRand.nextInt(55) + 170,
                            itemRand.nextInt(25) + 90,
                            itemRand.nextInt(40) + 130};
            default -> new int[]{0, 0, 0, 0};
        };
    }


}


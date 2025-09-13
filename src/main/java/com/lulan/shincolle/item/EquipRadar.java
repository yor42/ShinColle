package com.lulan.shincolle.item;

import com.lulan.shincolle.reference.ID;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;

/**
 * meta:
 * 0:  Air Radar Mk.I
 * 1:  Air Radar Mk.II
 * 2:  Surface Radar Mk.I
 * 3:  Surface Radar Mk.II
 * 4:  Abyssal Sonar
 * 5:  Abyssal Air Radar
 * 6:  Abyssal Surface Radar
 * 7:  Abyssal Sonar Mk.II
 * 8:  Abyssal FCS + CIC
 */
public class EquipRadar extends BasicEquip {

    private static final String NAME = "EquipRadar";


    public EquipRadar() {
        super();
        this.setTranslationKey(NAME);
        this.setHasSubtypes(true);
    }

    /**
     * rearrange item order in creative tab
     */
    @Override
    public void getSubItems(CreativeTabs tab, NonNullList<ItemStack> list) {
        if (this.isInCreativeTab(tab)) {
            //AA radar
            list.add(new ItemStack(this, 1, 0));
            list.add(new ItemStack(this, 1, 1));
            list.add(new ItemStack(this, 1, 5));
            //Surface radar
            list.add(new ItemStack(this, 1, 2));
            list.add(new ItemStack(this, 1, 3));
            list.add(new ItemStack(this, 1, 6));
            //sonar
            list.add(new ItemStack(this, 1, 4));
            list.add(new ItemStack(this, 1, 7));
            //other
            list.add(new ItemStack(this, 1, 8));
        }
    }

    @Override
    public int getTypes() {
        return 9;
    }

    @Override
    public int getEquipTypeIDFromMeta(int meta) {
        return switch (meta) {
            case 0, 1, 2, 3, 4 -> ID.EquipType.RADAR_LO;
            case 5, 6, 7, 8 -> ID.EquipType.RADAR_HI;
            default -> 0;
        };
    }

    @Override
    public int getItemEnchantability(ItemStack stack) {
        return switch (this.getEquipTypeIDFromMeta(stack.getMetadata())) {
            case ID.EquipType.RADAR_LO -> 12;
            case ID.EquipType.RADAR_HI -> 15;
            default -> 9;
        };
    }

    @Override
    public int[] getResourceValue(int meta) {
        return switch (this.getEquipTypeIDFromMeta(meta)) {
            case ID.EquipType.RADAR_LO ->  //200
                    new int[]{itemRand.nextInt(7) + 12,
                            itemRand.nextInt(6) + 10,
                            itemRand.nextInt(5) + 9,
                            itemRand.nextInt(4) + 7};
            case ID.EquipType.RADAR_HI ->  //2000
                    new int[]{itemRand.nextInt(40) + 110,
                            itemRand.nextInt(35) + 90,
                            itemRand.nextInt(30) + 70,
                            itemRand.nextInt(25) + 50};
            default -> new int[]{0, 0, 0, 0};
        };
    }


}


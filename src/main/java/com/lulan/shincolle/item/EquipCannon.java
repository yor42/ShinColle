package com.lulan.shincolle.item;

import com.lulan.shincolle.reference.ID;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;

/**
 * meta:
 * 0:  5-Inch Single Cannon
 * 1:  6-Inch Single Cannon
 * 2:  5-Inch Twin Cannon
 * 3:  6-Inch Twin Rapid-Fire Cannon
 * 4:  5-Inch Twin Dual Purpose Cannon
 * 5:  12.5-Inch Twin Secondary Cannon
 * 6:  14-Inch Twin Cannon
 * 7:  16-Inch Twin Cannon
 * 8:  20-Inch Twin Cannon
 * 9:  8-Inch Triple Cannon
 * 10: 16-Inch Triple Cannon
 * 11: 15-Inch Fortress Gun
 * 12: 5-inch Coastal Gun
 * 13: 8-inch Long Range Twin Cannon
 * 14: 15-inch Quadruple Cannon
 * 15: 12-inch Triple Cannon
 * 16: 46cm Triple Cannon
 */
public class EquipCannon extends BasicEquip {

    private static final String NAME = "EquipCannon";


    public EquipCannon() {
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
            //S
            list.add(new ItemStack(this, 1, 0));
            list.add(new ItemStack(this, 1, 1));
            list.add(new ItemStack(this, 1, 12));
            //Tw
            list.add(new ItemStack(this, 1, 2));
            list.add(new ItemStack(this, 1, 3));
            list.add(new ItemStack(this, 1, 4));
            list.add(new ItemStack(this, 1, 13));
            list.add(new ItemStack(this, 1, 5));
            list.add(new ItemStack(this, 1, 6));
            list.add(new ItemStack(this, 1, 7));
            list.add(new ItemStack(this, 1, 8));
            //Tr
            list.add(new ItemStack(this, 1, 11));
            list.add(new ItemStack(this, 1, 9));
            list.add(new ItemStack(this, 1, 15));
            list.add(new ItemStack(this, 1, 10));
            list.add(new ItemStack(this, 1, 16));
            //Qu
            list.add(new ItemStack(this, 1, 14));
        }
    }

    /**
     * S=3, Tw=8, Tri=3
     */
    @Override
    public int getTypes() {
        return 17;
    }

    @Override
    public int getIconTypes() {
        return 3;
    }

    @Override
    public int getEquipTypeIDFromMeta(int meta) {
        return switch (meta) {
            case 2, 3, 4, 5, 13 -> ID.EquipType.CANNON_TW_LO;
            case 6, 7, 8 -> ID.EquipType.CANNON_TW_HI;
            case 9, 10, 11, 14, 15, 16 -> ID.EquipType.CANNON_TR;
            default -> ID.EquipType.CANNON_SI;
        };
    }

    @Override
    public int getIconFromDamage(int meta) {
        return switch (this.getEquipTypeIDFromMeta(meta)) {
            case ID.EquipType.CANNON_SI -> 0;    //single cannon
            case ID.EquipType.CANNON_TW_LO, ID.EquipType.CANNON_TW_HI -> 1;    //twin cannon
            case ID.EquipType.CANNON_TR -> 2;    //triple cannon
            default -> 3;
        };
    }

    @Override
    public int getItemEnchantability(ItemStack stack) {
        return switch (this.getEquipTypeIDFromMeta(stack.getMetadata())) {
            case ID.EquipType.CANNON_TW_LO -> 12;
            case ID.EquipType.CANNON_TW_HI -> 18;
            case ID.EquipType.CANNON_TR -> 25;
            default -> 9;
        };
    }

    @Override
    public int[] getResourceValue(int meta) {
        return switch (this.getEquipTypeIDFromMeta(meta)) {
            case ID.EquipType.CANNON_SI ->     //128
                    new int[]{itemRand.nextInt(4) + 5,
                            itemRand.nextInt(4) + 5,
                            itemRand.nextInt(5) + 11,
                            itemRand.nextInt(3) + 3};
            case ID.EquipType.CANNON_TW_LO ->  //320
                    new int[]{itemRand.nextInt(7) + 10,
                            itemRand.nextInt(7) + 10,
                            itemRand.nextInt(8) + 16,
                            itemRand.nextInt(6) + 6};
            case ID.EquipType.CANNON_TW_HI ->  //1600
                    new int[]{itemRand.nextInt(10) + 50,
                            itemRand.nextInt(15) + 70,
                            itemRand.nextInt(35) + 90,
                            itemRand.nextInt(20) + 80};
            case ID.EquipType.CANNON_TR ->     //4400
                    new int[]{itemRand.nextInt(60) + 170,
                            itemRand.nextInt(70) + 210,
                            itemRand.nextInt(80) + 250,
                            itemRand.nextInt(50) + 130};
            default -> new int[]{0, 0, 0, 0};
        };
    }


}
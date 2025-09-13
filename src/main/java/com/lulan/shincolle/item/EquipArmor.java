package com.lulan.shincolle.item;

import com.lulan.shincolle.reference.ID;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;

/**
 * meta:
 * 0: armor
 * 1: heavy armor
 * 2: Anti-torpedo Bulge (S)
 * 3: Anti-torpedo Bulge (M)
 * 4: Anti-torpedo Bulge (L)
 * 5: Anti-torpedo Belt Armor
 * 6: Abyssal Protection Bulkhead
 */
public class EquipArmor extends BasicEquip {

    private static final String NAME = "EquipArmor";


    public EquipArmor() {
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
            {
                //armor
                list.add(new ItemStack(this, 1, 0));
                list.add(new ItemStack(this, 1, 1));
                list.add(new ItemStack(this, 1, 5));
                //bulge
                list.add(new ItemStack(this, 1, 2));
                list.add(new ItemStack(this, 1, 3));
                list.add(new ItemStack(this, 1, 4));
                //bulkhead
                list.add(new ItemStack(this, 1, 6));
            }
        }
    }

    @Override
    public int getTypes() {
        return 7;
    }

    @Override
    public int getEquipTypeIDFromMeta(int meta) {
        return switch (meta) {
            case 0, 2, 3, 5 -> ID.EquipType.ARMOR_LO;
            case 1, 4, 6 -> ID.EquipType.ARMOR_HI;
            default -> 0;
        };
    }

    @Override
    public int getItemEnchantability(ItemStack stack) {
        return switch (this.getEquipTypeIDFromMeta(stack.getMetadata())) {
            case ID.EquipType.ARMOR_LO -> 9;
            case ID.EquipType.ARMOR_HI -> 20;
            default -> 9;
        };
    }

    @Override
    public int[] getResourceValue(int meta) {
        return switch (this.getEquipTypeIDFromMeta(meta)) {
            case ID.EquipType.ARMOR_LO ->  //80
                    new int[]{itemRand.nextInt(3) + 3,
                            itemRand.nextInt(4) + 4,
                            itemRand.nextInt(2) + 2,
                            itemRand.nextInt(2) + 2};
            case ID.EquipType.ARMOR_HI ->  //500
                    new int[]{itemRand.nextInt(15) + 35,
                            itemRand.nextInt(20) + 45,
                            itemRand.nextInt(10) + 25,
                            itemRand.nextInt(5) + 15};
            default -> new int[]{0, 0, 0, 0};
        };
    }


}



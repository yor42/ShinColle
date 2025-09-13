package com.lulan.shincolle.item;

import com.lulan.shincolle.reference.ID;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;

import java.util.List;
import java.util.Map;

/**
 * meta:
 * 0:  21inch Torpedo Mk.I
 * 1:  21inch Torpedo Mk.II
 * 2:  22inch Torpedo Mk.II
 * 3:  Cuttlefish Torpedo
 * 4:  High-Speed Torpedo
 * 5:  High-speed Abyssal Torpedo Mod.2
 * 6:  Abyssal Ambush Torpedo
 */
public class EquipTorpedo extends BasicEquip implements IShipEffectItem {

    private static final String NAME = "EquipTorpedo";


    public EquipTorpedo() {
        super();
        this.setTranslationKey(NAME);
        this.setHasSubtypes(true);
    }

    @Override
    public int getTypes() {
        return 7;
    }

    @Override
    public int getEquipTypeIDFromMeta(int meta) {
        return switch (meta) {
            case 0, 1, 2 -> ID.EquipType.TORPEDO_LO;
            case 3, 4, 5, 6 -> ID.EquipType.TORPEDO_HI;
            default -> 0;
        };
    }

    @Override
    public int getItemEnchantability(ItemStack stack) {
        return switch (this.getEquipTypeIDFromMeta(stack.getMetadata())) {
            case ID.EquipType.TORPEDO_LO -> 16;
            case ID.EquipType.TORPEDO_HI -> 22;
            default -> 9;
        };
    }

    @Override
    public int[] getResourceValue(int meta) {
        return switch (this.getEquipTypeIDFromMeta(meta)) {
            case ID.EquipType.TORPEDO_LO ->  //160
                    new int[]{itemRand.nextInt(4) + 8,
                            itemRand.nextInt(5) + 8,
                            itemRand.nextInt(6) + 12,
                            itemRand.nextInt(4) + 5};
            case ID.EquipType.TORPEDO_HI ->  //1200
                    new int[]{itemRand.nextInt(20) + 60,
                            itemRand.nextInt(25) + 70,
                            itemRand.nextInt(30) + 80,
                            itemRand.nextInt(15) + 45};
            default -> new int[]{0, 0, 0, 0};
        };
    }

    @Override
    public Map<Integer, int[]> getEffectOnAttack(int meta) {
        return null;
    }

    //specific missile type by meta
    @Override
    public int getMissileType(int meta) {
        return -1;
    }

    //specific move type by meta
    @Override
    public int getMissileMoveType(int meta) {
        return -1;
    }

    @Override
    public int getMissileSpeedLevel(int meta) {
        return switch (meta) {
            case 3, 4 -> 1;
            case 5 -> 2;
            case 6 -> 3;
            default -> 0;
        };
    }

    @Override
    public void addInformation(ItemStack stack, World world, List<String> list, ITooltipFlag par4) {
        super.addInformation(stack, world, list, par4);

        int level = getMissileSpeedLevel(stack.getMetadata());

        if (level != 0) {
            list.add(TextFormatting.YELLOW + I18n.format("gui.shincolle:equip.torpedospeed", level));
        }
    }


}
package com.lulan.shincolle.intermod.ic2;

import com.lulan.shincolle.capability.CapaShipInventory;
import com.lulan.shincolle.client.gui.inventory.ContainerShipInventory;
import com.lulan.shincolle.entity.BasicEntityShip;
import com.lulan.shincolle.proxy.CommonProxy;
import com.lulan.shincolle.tileentity.TileEntityCrane;
import com.lulan.shincolle.utility.TileEntityHelper;
import ic2.api.item.ElectricItem;
import ic2.api.item.IElectricItem;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;

public class IC2EnergyUtil {

    public static double tryChargeContainerIC2(BasicEntityShip ship, TileEntityCrane crane) {
        CapaShipInventory inv = ship.getCapaShipInventory();
        ItemStack stack;
        double totalamount = 0;

        //fill all container in inventory
        for (int i = ContainerShipInventory.SLOTS_SHIPINV; i < inv.getSizeInventoryPaged(); i++) {
            stack = inv.getStackInSlotWithPageCheck(i);
            Item item = stack.getItem();
            if (stack.isEmpty() || stack.getCount() > 1 || !(item instanceof IElectricItem electricItem)) {
                continue;
            }

            double chargeamount = ElectricItem.manager.charge(stack, crane.getEUTransferExtract(), crane.getSourceTier(), false, true);

            if (chargeamount <= 0) {
                continue;
            }

            double finaltransaction = ElectricItem.manager.charge(stack, chargeamount, crane.getSourceTier(), false, false);
            crane.drawEnergy(finaltransaction);
            totalamount += finaltransaction;
        }//end for all slots

        return totalamount;
    }

    public static double tryDischargeContainerIC2(BasicEntityShip ship, TileEntityCrane crane) {
        CapaShipInventory inv = ship.getCapaShipInventory();
        double totalamount = 0;
        ItemStack stack;

        //fill all container in inventory
        for (int i = ContainerShipInventory.SLOTS_SHIPINV; i < inv.getSizeInventoryPaged(); i++) {
            stack = inv.getStackInSlotWithPageCheck(i);
            Item item = stack.getItem();
            if (stack.isEmpty() || stack.getCount() > 1 || !(item instanceof IElectricItem electricItem)) {
                continue;
            }

            double transfer = ElectricItem.manager.discharge(stack, crane.getEUTransferInsert(), crane.getSourceTier(), false, false, true);

            if (transfer <= 0) {
                continue;
            }

            double finaltransaction = ElectricItem.manager.discharge(stack, crane.getEUTransferInsert(), crane.getSourceTier(), false, false, false);
            crane.addEnergy((int) finaltransaction);
            totalamount += finaltransaction;
        }//end for all slots

        return totalamount;
    }

    public static boolean checkEnergyFillingFinishedEU(IInventory inv, boolean checkFull) {
        if (inv == null) return true;

        //inventory is ship inv
        if (inv instanceof CapaShipInventory shipInv) {

            for (int i = ContainerShipInventory.SLOTS_SHIPINV; i < shipInv.getSizeInventoryPaged(); i++) {

                if (checkEU(shipInv.getStackInSlotWithPageCheck(i), checkFull)) {
                    return false;
                }
            }
        }
        //inventory is vanilla chest
        else if (inv instanceof TileEntityChest) {
            //check main chest
            for (int i = 0; i < inv.getSizeInventory(); i++) {
                //check all slots are full
                if (checkEU(inv.getStackInSlot(i), checkFull)) {
                    return false;
                }

            }

            //check adj chest
            TileEntityChest chest2 = TileEntityHelper.getAdjChest((TileEntityChest) inv);

            if (chest2 != null) {
                for (int i = 0; i < chest2.getSizeInventory(); i++) {
                    if (checkEU(chest2.getStackInSlot(i), checkFull)) {
                        return false;
                    }
                }
            }
        }
        //other inventory
        else {
            for (int i = 0; i < inv.getSizeInventory(); i++) {
                //check all slots are full
                if (checkEU(inv.getStackInSlot(i), checkFull)) {
                    return false;
                }
            }
        }

        return true;
    }

    public static boolean checkEU(ItemStack stack, boolean checkFull) {
        if (!stack.isEmpty()) {

            return checkFull ? ElectricItem.manager.charge(stack, Double.MAX_VALUE, 4, true, true) > 0 : ElectricItem.manager.discharge(stack, Double.MAX_VALUE, 4, true, false, true) > 0;

        }
        return true;
    }
}

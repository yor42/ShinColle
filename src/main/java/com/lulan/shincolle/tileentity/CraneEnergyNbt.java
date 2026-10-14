package com.lulan.shincolle.tileentity;

import net.minecraft.nbt.NBTTagCompound;

/**
 * Minecraft-light NBT load rules for Crane EU storage (issue #14). Key names are unchanged.
 */
final class CraneEnergyNbt {

    private CraneEnergyNbt() {}

    static double loadEUCapacity(NBTTagCompound nbt, double defaultCapacity) {
        return nbt.hasKey("energy_EUCapacity") ? nbt.getDouble("energy_EUCapacity") : defaultCapacity;
    }

    static double loadEUEnergy(NBTTagCompound nbt, double capacity) {
        return Math.max(0D, Math.min(capacity, nbt.getDouble("energy_EUEnergy")));
    }
}

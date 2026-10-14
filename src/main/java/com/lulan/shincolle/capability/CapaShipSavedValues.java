package com.lulan.shincolle.capability;

import com.lulan.shincolle.entity.BasicEntityShip;
import com.lulan.shincolle.reference.ID;
import com.lulan.shincolle.reference.unitclass.Attrs;
import com.lulan.shincolle.utility.LogHelper;
import com.lulan.shincolle.utility.NBTHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.Constants;


/**
 * ship NBT values handler
 * this is NOT a really capability, just a value handler
 */
public class CapaShipSavedValues {

    public static final String SHIP_EXTPROP_NAME = "ShipExtProps";
    public static final String tagName = "ShipInv";    //ship inventory nbt tag
    private final int inventoryPage = 0;


    //save extend entity prop
    public static void saveNBTData(NBTTagCompound nbt, BasicEntityShip ship) {
        //save nbt data
        NBTTagCompound nbtExt = new NBTTagCompound();
        NBTTagCompound nbtExt_add0 = new NBTTagCompound();
        NBTTagCompound nbtExt_add2 = new NBTTagCompound();
        NBTTagCompound nbtExt_add3 = new NBTTagCompound();
        NBTTagCompound nbtExt_add4 = new NBTTagCompound();
        NBTTagCompound nbtExt_add5 = new NBTTagCompound();

        //save values to NBT
        nbtExt.setTag("Minor", nbtExt_add0);
        nbtExt_add0.setInteger("Level", ship.getStateMinor(ID.M.ShipLevel));
        nbtExt_add0.setInteger("Kills", ship.getStateMinor(ID.M.Kills));
        nbtExt_add0.setInteger("Exp", ship.getStateMinor(ID.M.ExpCurrent));
        nbtExt_add0.setInteger("NumAmmoL", ship.getStateMinor(ID.M.NumAmmoLight));
        nbtExt_add0.setInteger("NumAmmoH", ship.getStateMinor(ID.M.NumAmmoHeavy));
        nbtExt_add0.setInteger("NumGrudge", ship.getStateMinor(ID.M.NumGrudge));
        nbtExt_add0.setInteger("NumAirL", ship.getStateMinor(ID.M.NumAirLight));
        nbtExt_add0.setInteger("NumAirH", ship.getStateMinor(ID.M.NumAirHeavy));
        nbtExt_add0.setInteger("FMin", ship.getStateMinor(ID.M.FollowMin));
        nbtExt_add0.setInteger("FMax", ship.getStateMinor(ID.M.FollowMax));
        nbtExt_add0.setInteger("FHP", ship.getStateMinor(ID.M.FleeHP));
        nbtExt_add0.setInteger("GuardX", ship.getStateMinor(ID.M.GuardX));
        nbtExt_add0.setInteger("GuardY", ship.getStateMinor(ID.M.GuardY));
        nbtExt_add0.setInteger("GuardZ", ship.getStateMinor(ID.M.GuardZ));
        nbtExt_add0.setInteger("GuardDim", ship.getStateMinor(ID.M.GuardDim));
        nbtExt_add0.setInteger("GuardID", ship.getStateMinor(ID.M.GuardID));
        nbtExt_add0.setInteger("GuardType", ship.getStateMinor(ID.M.GuardType));
        nbtExt_add0.setInteger("PlayerUID", ship.getStateMinor(ID.M.PlayerUID));
        nbtExt_add0.setInteger("ShipUID", ship.getStateMinor(ID.M.ShipUID));
        nbtExt_add0.setInteger("FType", ship.getStateMinor(ID.M.FormatType));
        nbtExt_add0.setInteger("FPos", ship.getStateMinor(ID.M.FormatPos));
        nbtExt_add0.setInteger("Morale", ship.getStateMinor(ID.M.Morale));
        nbtExt_add0.setInteger("Food", ship.getStateMinor(ID.M.Food));
        nbtExt_add0.setInteger("Crane", ship.getStateMinor(ID.M.CraneState));
        nbtExt_add0.setInteger("WpStay", ship.getStateMinor(ID.M.WpStay));
        nbtExt_add0.setInteger("AutoCR", ship.getStateMinor(ID.M.UseCombatRation));
        nbtExt_add0.setInteger("Task", ship.getStateMinor(ID.M.Task));
        nbtExt_add0.setInteger("Side", ship.getStateMinor(ID.M.TaskSide));
        nbtExt_add0.setString("tagName", ship.getCustomNameTag());

        //save EntityState
        nbtExt.setTag("Display", nbtExt_add2);
        nbtExt_add2.setInteger("State", ship.getStateEmotion(ID.S.State));
        nbtExt_add2.setInteger("Emotion", ship.getStateEmotion(ID.S.Emotion));
        nbtExt_add2.setInteger("Emotion2", ship.getStateEmotion(ID.S.Emotion2));
        nbtExt_add2.setInteger("Phase", ship.getStateEmotion(ID.S.Phase));

        //save BonusPoint
        nbtExt.setTag("Point", nbtExt_add3);
        nbtExt_add3.setByte("HP", ship.getAttrs().getAttrsBonus(ID.AttrsBase.HP));
        nbtExt_add3.setByte("ATK", ship.getAttrs().getAttrsBonus(ID.AttrsBase.ATK));
        nbtExt_add3.setByte("DEF", ship.getAttrs().getAttrsBonus(ID.AttrsBase.DEF));
        nbtExt_add3.setByte("SPD", ship.getAttrs().getAttrsBonus(ID.AttrsBase.SPD));
        nbtExt_add3.setByte("MOV", ship.getAttrs().getAttrsBonus(ID.AttrsBase.MOV));
        nbtExt_add3.setByte("HIT", ship.getAttrs().getAttrsBonus(ID.AttrsBase.HIT));

        //save EntityFlag
        nbtExt.setTag("ShipFlags", nbtExt_add4);
        nbtExt_add4.setBoolean("CanFloat", ship.getStateFlag(ID.F.CanFloatUp));
        nbtExt_add4.setBoolean("IsMarried", ship.getStateFlag(ID.F.IsMarried));
        nbtExt_add4.setBoolean("NoFuel", ship.getStateFlag(ID.F.NoFuel));
        nbtExt_add4.setBoolean("Melee", ship.getStateFlag(ID.F.UseMelee));
        nbtExt_add4.setBoolean("AmmoL", ship.getStateFlag(ID.F.UseAmmoLight));
        nbtExt_add4.setBoolean("AmmoH", ship.getStateFlag(ID.F.UseAmmoHeavy));
        nbtExt_add4.setBoolean("AirL", ship.getStateFlag(ID.F.UseAirLight));
        nbtExt_add4.setBoolean("AirH", ship.getStateFlag(ID.F.UseAirHeavy));
        nbtExt_add4.setBoolean("WedEffect", ship.getStateFlag(ID.F.UseRingEffect));
        nbtExt_add4.setBoolean("CanDrop", ship.getStateFlag(ID.F.CanDrop));
        nbtExt_add4.setBoolean("CanFollow", ship.getStateFlag(ID.F.CanFollow));
        nbtExt_add4.setBoolean("OnSight", ship.getStateFlag(ID.F.OnSightChase));
        nbtExt_add4.setBoolean("PVPFirst", ship.getStateFlag(ID.F.PVPFirst));
        nbtExt_add4.setBoolean("AA", ship.getStateFlag(ID.F.AntiAir));
        nbtExt_add4.setBoolean("ASM", ship.getStateFlag(ID.F.AntiSS));
        nbtExt_add4.setBoolean("PassiveAI", ship.getStateFlag(ID.F.PassiveAI));
        nbtExt_add4.setBoolean("TimeKeeper", ship.getStateFlag(ID.F.TimeKeeper));
        nbtExt_add4.setBoolean("PickItem", ship.getStateFlag(ID.F.PickItem));
        nbtExt_add4.setBoolean("HeldItem", ship.getStateFlag(ID.F.ShowHeldItem));
        nbtExt_add4.setBoolean("AutoPump", ship.getStateFlag(ID.F.AutoPump));
        nbtExt_add4.setBoolean("IsMorph", ship.isMorph());

        //save timer
        nbtExt.setTag("Timer", nbtExt_add5);
        nbtExt_add5.setInteger("Crane", ship.getStateTimer(ID.T.CraneTime));

        //save owner name
        if (ship.ownerName != null && !ship.ownerName.isEmpty()) {
            nbtExt.setString("Owner", ship.ownerName);
        }

        //save unit name
        nbtExt = NBTHelper.saveStringTagArrayList(nbtExt, "uname", ship.unitNames);

        nbt.setTag(SHIP_EXTPROP_NAME, nbtExt);

        LogHelper.debug("DEBUG: save entity ExtNBT data on id: " + ship.getEntityId());
    }

    //load extend entity prop
    public static void loadNBTData(NBTTagCompound nbt, BasicEntityShip ship) {
        NBTTagCompound nbt_tag = getSection(nbt, SHIP_EXTPROP_NAME);

        //no saved ship data (ex: bare /summon): keep constructor defaults, nothing to recalc
        if (nbt_tag == null) {
            LogHelper.debug("DEBUG : no ExtNBT data, keep defaults on id: " + ship.getEntityId());
            return;
        }

        //each section is optional: a missing one keeps the current (default) values
        loadMinor(getSection(nbt_tag, "Minor"), ship);
        loadDisplay(getSection(nbt_tag, "Display"), ship);
        loadPoint(getSection(nbt_tag, "Point"), ship);
        loadFlags(getSection(nbt_tag, "ShipFlags"), ship);
        loadTimer(getSection(nbt_tag, "Timer"), ship);

        //load owner name
        String name = nbt_tag.getString("Owner");

        if (!name.isEmpty()) {
            ship.ownerName = name;
        } else {
            //load owner name fail, get owner again
            Entity owner = ship.getHostEntity();

            if (owner instanceof EntityPlayer) {
                ship.ownerName = owner.getName();
            }
        }

        //load unit name
        ship.unitNames = NBTHelper.loadStringTagArrayList(nbt_tag, "uname");

        //calc equip and attribute
        ship.setExpNext();    //for gui display
        ship.calcShipAttributes(31, true);    //re-calc attributes and send sync packet

        LogHelper.debug("DEBUG : load entity ExtNBT data on id: " + ship.getEntityId());
    }

    /** return the compound child, or null if the key is absent or not a compound */
    static NBTTagCompound getSection(NBTTagCompound parent, String key) {
        if (parent == null || !parent.hasKey(key, Constants.NBT.TAG_COMPOUND)) return null;
        return parent.getCompoundTag(key);
    }

    /** load default for a missing Level key (issue #7, #30): never 0 */
    static final int DEFAULT_LEVEL = 1;
    /** load default for a missing Morale key (issue #30): the spawn value of the constructor */
    static final int DEFAULT_MORALE = 60;
    /** load default for a missing CanDrop key (issues #30, #45): ships drop items by default */
    static final boolean DEFAULT_CAN_DROP = true;

    /** saved Level if the key exists, else 1 (issue #7: a stub Minor section must not give Level 0) */
    static int defaultedLevel(NBTTagCompound minor) {
        return intOr(minor, "Level", DEFAULT_LEVEL);
    }

    /**
     * saved int if the key exists, else the fallback (issue #30). A saved 0 is a real value and is never replaced.
     * Callers pass the ship's current value (constructor default on a fresh ship) as fallback.
     */
    static int intOr(NBTTagCompound nbt, String key, int fallback) {
        return nbt.hasKey(key) ? nbt.getInteger(key) : fallback;
    }

    /** saved byte if the key exists, else the fallback (issue #30) */
    static byte byteOr(NBTTagCompound nbt, String key, byte fallback) {
        return nbt.hasKey(key) ? nbt.getByte(key) : fallback;
    }

    /** saved boolean if the key exists, else the fallback (issue #30). A saved false is a real value. */
    static boolean boolOr(NBTTagCompound nbt, String key, boolean fallback) {
        return nbt.hasKey(key) ? nbt.getBoolean(key) : fallback;
    }

    /**
     * Per-key load defaults (issues #30, #45): every key is presence-checked. A missing key keeps the ship's current
     * value, which on a fresh ship is the BasicEntityShip constructor value; Level, Morale and CanDrop use the
     * explicit defaults above. A present key (including 0 / false) loads exactly as before.
     */
    private static void loadMinor(NBTTagCompound nbt_load, BasicEntityShip ship) {
        if (nbt_load == null) return;

        ship.setStateMinor(ID.M.ShipLevel, defaultedLevel(nbt_load));
        ship.setStateMinor(ID.M.Kills, intOr(nbt_load, "Kills", ship.getStateMinor(ID.M.Kills)));
        ship.setStateMinor(ID.M.ExpCurrent, intOr(nbt_load, "Exp", ship.getStateMinor(ID.M.ExpCurrent)));
        ship.setStateMinor(ID.M.NumAmmoLight, intOr(nbt_load, "NumAmmoL", ship.getStateMinor(ID.M.NumAmmoLight)));
        ship.setStateMinor(ID.M.NumAmmoHeavy, intOr(nbt_load, "NumAmmoH", ship.getStateMinor(ID.M.NumAmmoHeavy)));
        ship.setStateMinor(ID.M.NumGrudge, intOr(nbt_load, "NumGrudge", ship.getStateMinor(ID.M.NumGrudge)));
        ship.setStateMinor(ID.M.NumAirLight, intOr(nbt_load, "NumAirL", ship.getStateMinor(ID.M.NumAirLight)));
        ship.setStateMinor(ID.M.NumAirHeavy, intOr(nbt_load, "NumAirH", ship.getStateMinor(ID.M.NumAirHeavy)));
        ship.setStateMinor(ID.M.FollowMin, intOr(nbt_load, "FMin", ship.getStateMinor(ID.M.FollowMin)));
        ship.setStateMinor(ID.M.FollowMax, intOr(nbt_load, "FMax", ship.getStateMinor(ID.M.FollowMax)));
        ship.setStateMinor(ID.M.FleeHP, intOr(nbt_load, "FHP", ship.getStateMinor(ID.M.FleeHP)));
        ship.setStateMinor(ID.M.GuardX, intOr(nbt_load, "GuardX", ship.getStateMinor(ID.M.GuardX)));
        ship.setStateMinor(ID.M.GuardY, intOr(nbt_load, "GuardY", ship.getStateMinor(ID.M.GuardY)));
        ship.setStateMinor(ID.M.GuardZ, intOr(nbt_load, "GuardZ", ship.getStateMinor(ID.M.GuardZ)));
        ship.setStateMinor(ID.M.GuardDim, intOr(nbt_load, "GuardDim", ship.getStateMinor(ID.M.GuardDim)));
        ship.setStateMinor(ID.M.GuardID, intOr(nbt_load, "GuardID", ship.getStateMinor(ID.M.GuardID)));
        ship.setStateMinor(ID.M.GuardType, intOr(nbt_load, "GuardType", ship.getStateMinor(ID.M.GuardType)));
        ship.setStateMinor(ID.M.PlayerUID, intOr(nbt_load, "PlayerUID", ship.getStateMinor(ID.M.PlayerUID)));
        ship.setStateMinor(ID.M.ShipUID, intOr(nbt_load, "ShipUID", ship.getStateMinor(ID.M.ShipUID)));
        ship.setStateMinor(ID.M.FormatType, intOr(nbt_load, "FType", ship.getStateMinor(ID.M.FormatType)));
        ship.setStateMinor(ID.M.FormatPos, intOr(nbt_load, "FPos", ship.getStateMinor(ID.M.FormatPos)));
        ship.setStateMinor(ID.M.Morale, intOr(nbt_load, "Morale", DEFAULT_MORALE));
        ship.setStateMinor(ID.M.Food, intOr(nbt_load, "Food", ship.getStateMinor(ID.M.Food)));
        ship.setStateMinor(ID.M.CraneState, intOr(nbt_load, "Crane", ship.getStateMinor(ID.M.CraneState)));
        ship.setStateMinor(ID.M.WpStay, intOr(nbt_load, "WpStay", ship.getStateMinor(ID.M.WpStay)));
        ship.setStateMinor(ID.M.UseCombatRation, intOr(nbt_load, "AutoCR", ship.getStateMinor(ID.M.UseCombatRation)));
        ship.setStateMinor(ID.M.Task, intOr(nbt_load, "Task", ship.getStateMinor(ID.M.Task)));
        ship.setStateMinor(ID.M.TaskSide, intOr(nbt_load, "Side", ship.getStateMinor(ID.M.TaskSide)));
        if (nbt_load.hasKey("tagName")) ship.setNameTag(nbt_load.getString("tagName"));

    }

    private static void loadDisplay(NBTTagCompound nbt_load, BasicEntityShip ship) {
        if (nbt_load == null) return;

        //load display state

        ship.setStateEmotion(ID.S.State, intOr(nbt_load, "State", ship.getStateEmotion(ID.S.State)), false);
        ship.setStateEmotion(ID.S.Emotion, intOr(nbt_load, "Emotion", ship.getStateEmotion(ID.S.Emotion)), false);
        ship.setStateEmotion(ID.S.Emotion2, intOr(nbt_load, "Emotion2", ship.getStateEmotion(ID.S.Emotion2)), false);
        ship.setStateEmotion(ID.S.Phase, intOr(nbt_load, "Phase", ship.getStateEmotion(ID.S.Phase)), false);

    }

    private static void loadPoint(NBTTagCompound nbt_load, BasicEntityShip ship) {
        if (nbt_load == null) return;

        //load bonus point

        Attrs attrs = ship.getAttrs();
        attrs.setAttrsBonus(ID.AttrsBase.HP, byteOr(nbt_load, "HP", attrs.getAttrsBonus(ID.AttrsBase.HP)));
        attrs.setAttrsBonus(ID.AttrsBase.ATK, byteOr(nbt_load, "ATK", attrs.getAttrsBonus(ID.AttrsBase.ATK)));
        attrs.setAttrsBonus(ID.AttrsBase.DEF, byteOr(nbt_load, "DEF", attrs.getAttrsBonus(ID.AttrsBase.DEF)));
        attrs.setAttrsBonus(ID.AttrsBase.SPD, byteOr(nbt_load, "SPD", attrs.getAttrsBonus(ID.AttrsBase.SPD)));
        attrs.setAttrsBonus(ID.AttrsBase.MOV, byteOr(nbt_load, "MOV", attrs.getAttrsBonus(ID.AttrsBase.MOV)));
        attrs.setAttrsBonus(ID.AttrsBase.HIT, byteOr(nbt_load, "HIT", attrs.getAttrsBonus(ID.AttrsBase.HIT)));

    }

    private static void loadFlags(NBTTagCompound nbt_load, BasicEntityShip ship) {
        if (nbt_load == null) return;

        //load flags

        loadFlag(nbt_load, "CanFloat", ID.F.CanFloatUp, ship);
        loadFlag(nbt_load, "IsMarried", ID.F.IsMarried, ship);
        loadFlag(nbt_load, "NoFuel", ID.F.NoFuel, ship);
        loadFlag(nbt_load, "Melee", ID.F.UseMelee, ship);
        loadFlag(nbt_load, "AmmoL", ID.F.UseAmmoLight, ship);
        loadFlag(nbt_load, "AmmoH", ID.F.UseAmmoHeavy, ship);
        loadFlag(nbt_load, "AirL", ID.F.UseAirLight, ship);
        loadFlag(nbt_load, "AirH", ID.F.UseAirHeavy, ship);
        loadFlag(nbt_load, "WedEffect", ID.F.UseRingEffect, ship);
        ship.setStateFlag(ID.F.CanDrop, boolOr(nbt_load, "CanDrop", DEFAULT_CAN_DROP));
        loadFlag(nbt_load, "CanFollow", ID.F.CanFollow, ship);
        loadFlag(nbt_load, "OnSight", ID.F.OnSightChase, ship);
        loadFlag(nbt_load, "PVPFirst", ID.F.PVPFirst, ship);
        loadFlag(nbt_load, "AA", ID.F.AntiAir, ship);
        loadFlag(nbt_load, "ASM", ID.F.AntiSS, ship);
        loadFlag(nbt_load, "PassiveAI", ID.F.PassiveAI, ship);
        loadFlag(nbt_load, "TimeKeeper", ID.F.TimeKeeper, ship);
        loadFlag(nbt_load, "PickItem", ID.F.PickItem, ship);
        loadFlag(nbt_load, "HeldItem", ID.F.ShowHeldItem, ship);
        loadFlag(nbt_load, "AutoPump", ID.F.AutoPump, ship);
        ship.setIsMorph(boolOr(nbt_load, "IsMorph", ship.isMorph()));

    }

    /** presence-checked flag load: a missing key keeps the ship's current (constructor) value */
    private static void loadFlag(NBTTagCompound nbt_load, String key, int flag, BasicEntityShip ship) {
        ship.setStateFlag(flag, boolOr(nbt_load, key, ship.getStateFlag(flag)));
    }

    private static void loadTimer(NBTTagCompound nbt_load, BasicEntityShip ship) {
        if (nbt_load == null) return;

        //load timer
        ship.setStateTimer(ID.T.CraneTime, intOr(nbt_load, "Crane", ship.getStateTimer(ID.T.CraneTime)));
    }

}

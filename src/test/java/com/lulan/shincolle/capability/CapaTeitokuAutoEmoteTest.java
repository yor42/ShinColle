package com.lulan.shincolle.capability;

import net.minecraft.nbt.NBTTagCompound;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Issue #32 (ADR-0002, additive key): AutoEmoteOff absent = auto emote on, so saves written before the key
 * existed keep emoting. The key is saved and loaded with the other player boolean settings.
 */
class CapaTeitokuAutoEmoteTest {

    @Test
    void keyNameIsStable() {
        assertEquals("AutoEmoteOff", CapaTeitoku.AUTO_EMOTE_OFF_KEY);
    }

    @Test
    void absentKeyMeansEmoteOn() {
        assertFalse(CapaTeitoku.readAutoEmoteOff(new NBTTagCompound()));
        assertFalse(new CapaTeitoku().isAutoEmoteOff());
    }

    @Test
    void oldSaveWithoutKeyLoadsAsEmoteOn() {
        NBTTagCompound ext = new NBTTagCompound();
        ext.setBoolean("hasRing", true);
        NBTTagCompound root = new NBTTagCompound();
        root.setTag(CapaTeitoku.CAPA_KEY, ext);

        CapaTeitoku capa = new CapaTeitoku();
        capa.setAutoEmoteOff(true);
        capa.loadNBTData(root);
        assertFalse(capa.isAutoEmoteOff());
    }

    @Test
    void savedTrueLoadsAsEmoteOff() {
        NBTTagCompound ext = new NBTTagCompound();
        ext.setBoolean(CapaTeitoku.AUTO_EMOTE_OFF_KEY, true);
        assertTrue(CapaTeitoku.readAutoEmoteOff(ext));
    }

    @Test
    void flagRoundTripsThroughSaveAndLoad() {
        CapaTeitoku src = new CapaTeitoku();
        src.setAutoEmoteOff(true);
        NBTTagCompound nbt = src.saveNBTData(new NBTTagCompound());
        assertTrue(nbt.getCompoundTag(CapaTeitoku.CAPA_KEY).hasKey(CapaTeitoku.AUTO_EMOTE_OFF_KEY));

        CapaTeitoku dst = new CapaTeitoku();
        dst.loadNBTData(nbt);
        assertTrue(dst.isAutoEmoteOff());

        src.setAutoEmoteOff(false);
        CapaTeitoku dst2 = new CapaTeitoku();
        dst2.setAutoEmoteOff(true);
        dst2.loadNBTData(src.saveNBTData(new NBTTagCompound()));
        assertFalse(dst2.isAutoEmoteOff());
    }
}

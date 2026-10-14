package com.lulan.shincolle.handler;

import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AbstractAttributeMap;
import net.minecraft.entity.ai.attributes.AttributeMap;
import net.minecraft.init.Bootstrap;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTSizeTracker;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.util.Constants;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Issue #25 (RND-07): EventHandler.java:1134 and MetamorphHelper.java:346 test
 * {@code name == "generic.maxHealth"} on a string read from NBT.
 * <p>
 * MECHANISM EVIDENCE ONLY. The two production blocks are inline in Metamorph event handlers that
 * need a Metamorph SpawnGhostEvent / morph manager, so they cannot be called headlessly. This test
 * reproduces the exact comparison on the exact NBT shape the blocks read (the "Attributes" list
 * written by SharedMonsterAttributes.writeBaseAttributeMapToNBT) to show WHEN identity comparison
 * is true or false. Minimal seam to test the production code: extract the tag-list loop into a
 * static helper taking an NBTTagCompound.
 */
class AttributeNameIdentityComparisonTest {

    @BeforeAll
    static void bootstrapMinecraft() {
        Bootstrap.register();
    }

    private static NBTTagCompound entityTagWithMaxHealth(double base) {
        AbstractAttributeMap map = new AttributeMap();
        map.registerAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(base);
        NBTTagCompound entity = new NBTTagCompound();
        entity.setTag("Attributes", SharedMonsterAttributes.writeBaseAttributeMapToNBT(map));
        return entity;
    }

    /** Verbatim copy of the loop condition from EventHandler / MetamorphHelper (using ==). */
    @SuppressWarnings("StringEquality")
    private static int trimWithIdentityComparison(NBTTagCompound entity) {
        int hits = 0;
        NBTTagList tagList = entity.getTagList("Attributes", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < tagList.tagCount(); i++) {
            NBTTagCompound tags = tagList.getCompoundTagAt(i);
            String name = tags.getString("Name");
            if (name == "generic.maxHealth") {
                tags.setDouble("Base", 20D);
                hits++;
            }
        }
        return hits;
    }

    private static NBTTagCompound diskRoundTrip(NBTTagCompound tag) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            CompressedStreamTools.write(tag, new DataOutputStream(bytes));
            return CompressedStreamTools.read(new DataInputStream(new ByteArrayInputStream(bytes.toByteArray())),
                    NBTSizeTracker.INFINITE);
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    private static double firstBase(NBTTagCompound entity) {
        return entity.getTagList("Attributes", Constants.NBT.TAG_COMPOUND).getCompoundTagAt(0).getDouble("Base");
    }

    @Test
    void identityComparisonMatchesOnFreshInMemoryNbtBecauseTheNameIsAnInternedLiteral() {
        // Characterization: a tag built from a live attribute map still holds the interned literal,
        // so == is TRUE (this is the shape MetamorphHelper builds from target.serializeNBT()).
        NBTTagCompound entity = entityTagWithMaxHealth(500D);

        assertEquals(1, trimWithIdentityComparison(entity));
        assertEquals(20D, firstBase(entity), 0D);
    }

    @Test
    void identityComparisonNeverMatchesOnNbtThatWasReadFromBytes_currentlyDeadForSavedMorphs() {
        // Characterization of the suspected defect (#25): NBT read from disk/network gets a new String
        // object, so == is FALSE, the Base=20 trim does not run and the 500 HP base survives.
        NBTTagCompound entity = diskRoundTrip(entityTagWithMaxHealth(500D));
        String name = entity.getTagList("Attributes", Constants.NBT.TAG_COMPOUND).getCompoundTagAt(0).getString("Name");
        assertEquals("generic.maxHealth", name);
        assertFalse(name == "generic.maxHealth", "a decoded string is a distinct object from the literal");

        assertEquals(0, trimWithIdentityComparison(entity));
        assertEquals(500D, firstBase(entity), 0D);
    }

    @Test
    void equalsWouldMatchTheDiskShapedNbtToo() {
        // What a future fix (.equals) changes: the disk-shaped NBT would then be trimmed as well.
        NBTTagCompound entity = diskRoundTrip(entityTagWithMaxHealth(500D));
        String name = entity.getTagList("Attributes", Constants.NBT.TAG_COMPOUND).getCompoundTagAt(0).getString("Name");
        assertTrue("generic.maxHealth".equals(name));
    }
}

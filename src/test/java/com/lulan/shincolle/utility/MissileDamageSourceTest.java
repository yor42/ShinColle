package com.lulan.shincolle.utility;

import net.minecraft.entity.Entity;
import net.minecraft.init.Bootstrap;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EntityDamageSource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Issue #40: MissileDamageSource must behave like DamageSource.causeMobDamage(null).setExplosion()
 * except for the missile marker that BuffHelper.isMissileDamage recognises.
 */
class MissileDamageSourceTest {

    private static class DummyEntity extends Entity {
        DummyEntity() {
            super(null);
        }

        @Override
        protected void entityInit() {
        }

        @Override
        protected void readEntityFromNBT(NBTTagCompound compound) {
        }

        @Override
        protected void writeEntityToNBT(NBTTagCompound compound) {
        }
    }

    @BeforeAll
    static void bootstrapMinecraft() {
        Bootstrap.register();
    }

    @Test
    void sameSemanticsAsMobDamageExplosion() {
        Entity host = new DummyEntity();
        DamageSource expected = DamageSource.causeMobDamage(null).setExplosion();
        DamageSource missile = new MissileDamageSource(host);

        assertEquals(expected.getDamageType(), missile.getDamageType());
        assertEquals("mob", missile.getDamageType());
        assertSame(host, missile.getTrueSource());
        assertSame(host, missile.getImmediateSource());
        assertEquals(expected.isExplosion(), missile.isExplosion());
        assertTrue(missile.isExplosion());
        assertEquals(expected.isProjectile(), missile.isProjectile());
        assertEquals(expected.isUnblockable(), missile.isUnblockable());
        assertEquals(expected.isDamageAbsolute(), missile.isDamageAbsolute());
        assertEquals(expected.isDifficultyScaled(), missile.isDifficultyScaled());
        assertEquals(expected.isFireDamage(), missile.isFireDamage());
        assertEquals(expected.isMagicDamage(), missile.isMagicDamage());
        assertEquals(expected.canHarmInCreative(), missile.canHarmInCreative());
    }

    @Test
    void sameOwnerAndSourceAsPlainEntityDamageSource() {
        Entity host = new DummyEntity();
        DamageSource plain = new EntityDamageSource("mob", host).setExplosion();
        DamageSource missile = new MissileDamageSource(host);
        assertSame(plain.getTrueSource(), missile.getTrueSource());
        assertSame(plain.getImmediateSource(), missile.getImmediateSource());
    }

    @Test
    void buffHelperRecognisesMissileSourceOnly() {
        Entity host = new DummyEntity();
        assertTrue(BuffHelper.isMissileDamage(new MissileDamageSource(host)));
        assertFalse(BuffHelper.isMissileDamage(DamageSource.causeMobDamage(null)));
        assertFalse(BuffHelper.isMissileDamage(DamageSource.causeMobDamage(null).setProjectile()));
        assertFalse(BuffHelper.isMissileDamage(DamageSource.MAGIC));
    }
}

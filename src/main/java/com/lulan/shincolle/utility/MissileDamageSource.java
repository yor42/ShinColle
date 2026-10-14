package com.lulan.shincolle.utility;

import net.minecraft.entity.Entity;
import net.minecraft.util.EntityDamageSource;

/**
 * Damage source of ShinColle missile explosions (issue #40).
 * <p>
 * Behaves exactly like {@code DamageSource.causeMobDamage(host).setExplosion()}: damage type "mob",
 * owner and immediate source are the host entity (not the missile), same death message.
 * The only difference is the marker type, which lets {@link BuffHelper#applyBuffOnDamageByResist}
 * recognise missile hits (the immediate source is the host, so an instanceof check on the missile entity never matched).
 */
public class MissileDamageSource extends EntityDamageSource {

    public MissileDamageSource(Entity host) {
        super("mob", host);
        this.setExplosion();
    }
}

package com.lulan.shincolle.mixin;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.lulan.shincolle.entity.BasicEntityShip;
import com.lulan.shincolle.utility.SpawnSweepGuard;
import net.minecraft.entity.Entity;
import net.minecraft.world.WorldServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Issue #10 (ENT-13): with spawn-animals=false or spawn-npcs=false, vanilla
 * WorldServer.updateEntityWithOptionalForce calls setDead() on every EntityAnimal / EntityWaterMob / INpc.
 * Friendly ships extend EntityTameable (an animal), so they were deleted on the first tick.
 * Only the setDead calls inside this one method are wrapped (a MixinExtras wrapper, so it stacks with other mods); ships are skipped, everything else is unchanged.
 */
@Mixin(WorldServer.class)
public abstract class MixinWorldServer {

    @WrapWithCondition(method = "updateEntityWithOptionalForce",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;setDead()V"))
    private boolean shincolle$allowSweep(Entity entity) {
        return !SpawnSweepGuard.isExempt(entity.getClass(), BasicEntityShip.class);
    }
}

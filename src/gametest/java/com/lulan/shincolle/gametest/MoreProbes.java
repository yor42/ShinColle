package com.lulan.shincolle.gametest;

import com.lulan.shincolle.capability.CapaTeitoku;
import com.lulan.shincolle.entity.BasicEntityShip;
import com.lulan.shincolle.network.C2SGUIPackets;
import com.lulan.shincolle.reference.ID;
import com.lulan.shincolle.utility.BuffHelper;
import com.lulan.shincolle.utility.MissileDamageSource;
import com.lulan.shincolle.utility.TeamHelper;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.MobEffects;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EntityDamageSource;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;

/** probes addteam29, missile40, guard37 (dev harness only) */
final class MoreProbes {
    private MoreProbes() {
    }

    static String ids(BasicEntityShip s) {
        return "ship[playerUID=" + s.getPlayerUID() + " shipUID=" + s.getShipUID() + " ownerUUID=" + s.getOwnerId() + " ticks=" + s.ticksExisted + "]";
    }

    static int slotOf(CapaTeitoku c, BasicEntityShip s) {
        for (int i = 0; i < 6; i++) if (c.getShipEntityCurrentTeam(i) == s) return i;
        return -1;
    }

    /* ---------------- AddTeam isolation (#29) ---------------- */
    static final class AddTeam extends ServerProbes.Probe {
        BasicEntityShip ship;
        EntityPlayerMP owner, op, stranger;

        AddTeam() {
            super("addteam29");
        }

        @Override
        boolean step(WorldServer w, int t) throws Throwable {
            MinecraftServer server = w.getMinecraftServer();
            if (t == 40) {
                BlockPos sp = ServerProbes.spawnTop(w);
                double x = sp.getX() + 0.5, y = sp.getY(), z = sp.getZ() + 0.5;
                owner = ServerProbes.player(w, "AT-owner", 8101, x, y, z);
                op = ServerProbes.player(w, "AT-op", 8102, x + 1, y, z);
                stranger = ServerProbes.player(w, "AT-stranger", 8103, x + 2, y, z);
                server.getPlayerList().addOp(op.getGameProfile());
                ship = ServerProbes.spawnShip(w, "shincolle:EntityDestroyerI", "AT-ship", x, y, z + 3, null);
                ship.setPlayerUID(8101);
                ship.setOwnerId(owner.getUniqueID());
                info("t40 right after setPlayerUID: " + ids(ship));
                return false;
            }
            if (t == 41 || t == 100) info("t" + t + ": " + ids(ship) + " ownerCapaUID=" + CapaTeitoku.getTeitokuCapability(owner).getPlayerUID());
            if (t == 41) {
                //same timing as the auth29 probe: ship UID still -1
                CapaTeitoku c0 = CapaTeitoku.getTeitokuCapability(owner);
                c0.removeShipCurrentTeam();
                c0.addShipEntity(0, ship, false);
                int early = slotOf(c0, ship);
                info("early (shipUID=" + ship.getShipUID() + ") direct capa.addShipEntity by owner -> slot " + early);
                check("owner adds ship while shipUID is still -1", early >= 0, "slot=" + early);
                c0.removeShipCurrentTeam();
            }
            if (t != 100) return false;
            ServerProbes.Auth29 helper = new ServerProbes.Auth29();
            EntityPlayerMP[] actors = {owner, op, stranger};
            String[] names = {"owner", "OP", "stranger"};
            for (int i = 0; i < 3; i++) {
                CapaTeitoku c = CapaTeitoku.getTeitokuCapability(actors[i]);
                c.removeShipCurrentTeam();
                boolean same = TeamHelper.checkSameOwner(actors[i], ship);
                c.addShipEntity(0, ship, false);
                int direct = slotOf(c, ship);
                info(names[i] + ": capaUID=" + c.getPlayerUID() + " capa.player=" + (c.player == null ? "null" : c.player.getName()) + " checkSameOwner(player,ship)=" + same
                        + " direct capa.addShipEntity -> slot " + direct);
                c.removeShipCurrentTeam();
                Throwable th = helper.runGui(new C2SGUIPackets(actors[i], C2SGUIPackets.PID.AddTeam, ship.getEntityId()), actors[i]);
                int viaPacket = slotOf(c, ship);
                info(names[i] + ": via AddTeam packet -> slot " + viaPacket + (th == null ? "" : " handler threw " + th));
                if (i == 0) {
                    check("owner: direct capa.addShipEntity puts ship in team", direct >= 0, "slot=" + direct);
                    check("owner: AddTeam packet puts ship in team", viaPacket >= 0, "slot=" + viaPacket);
                } else if (i == 1) {
                    info("OP: packet gate (mayOwnShip) passes; end state: direct slot=" + direct + " packet slot=" + viaPacket);
                } else {
                    check("stranger: AddTeam packet does not add", viaPacket < 0, "slot=" + viaPacket);
                }
                c.removeShipCurrentTeam();
            }
            server.getPlayerList().removeOp(op.getGameProfile());
            ship.setDead();
            for (EntityPlayerMP p : actors) ServerProbes.unregister(w, p);
            return true;
        }
    }

    /* ---------------- missile resist (#40) ---------------- */
    static final class Missile extends ServerProbes.Probe {
        Missile() {
            super("missile40");
        }

        private void expect(String what, float got, float want) {
            check(what, Math.abs(got - want) < 0.01F, "damage=" + got + " expected=" + want);
        }

        @Override
        boolean step(WorldServer w, int t) throws Throwable {
            if (t != 40) return false;
            BlockPos sp = ServerProbes.spawnTop(w);
            double x = sp.getX() + 0.5, y = sp.getY(), z = sp.getZ() + 0.5;
            BasicEntityShip host = ServerProbes.spawnShip(w, "shincolle:EntityDestroyerI", "MS-host", x, y, z, null);
            BasicEntityShip tgt = ServerProbes.spawnShip(w, "shincolle:EntityDestroyerI", "MS-target", x + 4, y, z, null);
            host.setPlayerUID(8301);
            tgt.setPlayerUID(8302);
            info("potion ids: RESISTANCE=" + Potion.getIdFromPotion(MobEffects.RESISTANCE) + " FIRE_RESISTANCE=" + Potion.getIdFromPotion(MobEffects.FIRE_RESISTANCE)
                    + " (BuffHelper reads 11 for missile, 12 for non-missile ship attacks)");
            info("apply sites: BasicEntityShip.java:2448, BasicEntityShipHostile.java:480, BasicEntityMount.java:268, MetamorphHelper.java:246; missile source built at EntityAbyssMissile.java:572 / EntityFloatingFort.java:179");

            DamageSource missile = new MissileDamageSource(host);
            DamageSource direct = new EntityDamageSource("mob", host);
            info("isMissileDamage(MissileDamageSource)=" + BuffHelper.isMissileDamage(missile) + " isMissileDamage(plain EntityDamageSource(host))=" + BuffHelper.isMissileDamage(direct));

            tgt.addPotionEffect(new PotionEffect(MobEffects.RESISTANCE, 600, 0));
            BuffHelper.convertPotionToBuffMap(tgt);
            info("after RESISTANCE I on target: buffMap=" + tgt.getBuffMap());
            expect("missile vs RESISTANCE I (buff 11 lvl1)", BuffHelper.applyBuffOnDamageByResist(tgt, missile, 100F), 80F);
            expect("non-missile ship hit vs RESISTANCE I (must be unaffected)", BuffHelper.applyBuffOnDamageByResist(tgt, direct, 100F), 100F);

            tgt.removePotionEffect(MobEffects.RESISTANCE);
            tgt.addPotionEffect(new PotionEffect(MobEffects.FIRE_RESISTANCE, 600, 0));
            BuffHelper.convertPotionToBuffMap(tgt);
            info("after FIRE_RESISTANCE I only: buffMap=" + tgt.getBuffMap());
            expect("missile vs FIRE_RESISTANCE I only (must be unaffected)", BuffHelper.applyBuffOnDamageByResist(tgt, missile, 100F), 100F);
            expect("non-missile ship hit vs FIRE_RESISTANCE I", BuffHelper.applyBuffOnDamageByResist(tgt, direct, 100F), 80F);

            tgt.removePotionEffect(MobEffects.FIRE_RESISTANCE);
            tgt.addPotionEffect(new PotionEffect(MobEffects.RESISTANCE, 600, 5));
            BuffHelper.convertPotionToBuffMap(tgt);
            info("after RESISTANCE VI: buffMap=" + tgt.getBuffMap());
            expect("missile vs RESISTANCE VI (cap at level 4)", BuffHelper.applyBuffOnDamageByResist(tgt, missile, 100F), 20F);
            tgt.removePotionEffect(MobEffects.RESISTANCE);
            BuffHelper.convertPotionToBuffMap(tgt);
            expect("missile with no buff", BuffHelper.applyBuffOnDamageByResist(tgt, missile, 100F), 100F);

            double[] avg = new double[2];
            for (int pass = 0; pass < 2; pass++) {
                if (pass == 1) {
                    tgt.addPotionEffect(new PotionEffect(MobEffects.RESISTANCE, 6000, 3));
                    BuffHelper.convertPotionToBuffMap(tgt);
                }
                double sum = 0;
                for (int i = 0; i < 40; i++) {
                    tgt.setHealth(tgt.getMaxHealth());
                    tgt.hurtResistantTime = 0;
                    float before = tgt.getHealth();
                    tgt.attackEntityFrom(new MissileDamageSource(host), 6F);
                    sum += before - tgt.getHealth();
                }
                avg[pass] = sum / 40;
            }
            info(String.format("end-to-end missile hits via attackEntityFrom(6 dmg) x40: avg hp loss no buff=%.3f, RESISTANCE IV=%.3f (random dodge/DEF in the pipeline)", avg[0], avg[1]));
            check("end to end: RESISTANCE IV reduces average missile damage", avg[1] < avg[0], avg[1] + " < " + avg[0]);
            host.setDead();
            tgt.setDead();
            return true;
        }
    }

    /* ---------------- stationary guard (#37) ---------------- */
    static final class Guard extends ServerProbes.Probe {
        static final String[] NAMES = {"G1-type1-active", "G2-type0", "G3-type1-passive"};
        final BasicEntityShip[] ships = new BasicEntityShip[3];
        final EntityZombie[] zombies = new EntityZombie[3];
        final int[] ammo0 = new int[3];
        final boolean[] everTargeted = new boolean[3];

        Guard() {
            super("guard37");
        }

        @Override
        boolean step(WorldServer w, int t) throws Throwable {
            if (t == 40) {
                BlockPos sp = ServerProbes.spawnTop(w);
                for (int i = 0; i < 3; i++) {
                    double x = sp.getX() + 0.5 + 24 * i, y = sp.getY(), z = sp.getZ() + 0.5;
                    BasicEntityShip s = ServerProbes.spawnShip(w, "shincolle:EntityDestroyerI", NAMES[i], x, y, z, null);
                    s.setPlayerUID(8400 + i);
                    s.setStateMinor(ID.M.NumAmmoLight, 300);
                    s.setStateMinor(ID.M.NumAmmoHeavy, 100);
                    s.setStateMinor(ID.M.NumGrudge, 900);
                    s.setMorale(5000);
                    s.setStateFlag(ID.F.NoFuel, false);
                    s.setGuardedPos((int) Math.floor(x), (int) y, (int) Math.floor(z), 0, i == 1 ? 0 : 1);
                    s.setStateFlag(ID.F.PassiveAI, i == 2);
                    s.setStateFlag(ID.F.UseAmmoLight, true);
                    s.setStateFlag(ID.F.UseAmmoHeavy, true);
                    EntityZombie z1 = new EntityZombie(w);
                    z1.setNoAI(true);
                    z1.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(1000);
                    z1.setHealth(1000);
                    z1.addPotionEffect(new PotionEffect(MobEffects.FIRE_RESISTANCE, 100000, 0));
                    z1.setPosition(x, y, z + 7);
                    w.spawnEntity(z1);
                    ships[i] = s;
                    zombies[i] = z1;
                }
                return false;
            }
            if (t == 60) {
                for (int i = 0; i < 3; i++) {
                    ammo0[i] = ships[i].getStateMinor(ID.M.NumAmmoLight) + ships[i].getStateMinor(ID.M.NumAmmoHeavy);
                    info(NAMES[i] + " t60: guardType=" + ships[i].getStateMinor(ID.M.GuardType) + " passive=" + ships[i].getStateFlag(ID.F.PassiveAI) + " ammo=" + ammo0[i]
                            + " morale=" + ships[i].getMorale() + " noFuel=" + ships[i].getStateFlag(ID.F.NoFuel) + " dist=" + ships[i].getDistance(zombies[i]));
                }
            }
            if (t > 60) {
                for (int i = 0; i < 3; i++) if (ships[i].getEntityTarget() != null) everTargeted[i] = true;
            }
            if (t == 700) {
                for (int i = 0; i < 3; i++) {
                    BasicEntityShip s = ships[i];
                    int ammo = s.getStateMinor(ID.M.NumAmmoLight) + s.getStateMinor(ID.M.NumAmmoHeavy);
                    info(NAMES[i] + " t700: zombieHp=" + zombies[i].getHealth() + "/1000 ammo=" + ammo + " (was " + ammo0[i] + ") everTargeted=" + everTargeted[i]
                            + " guardType=" + s.getStateMinor(ID.M.GuardType) + " pos=" + (int) s.posX + "/" + (int) s.posZ + " morale=" + s.getMorale() + " noFuel=" + s.getStateFlag(ID.F.NoFuel));
                }
                boolean fired0 = zombies[0].getHealth() < 1000 || ships[0].getStateMinor(ID.M.NumAmmoLight) + ships[0].getStateMinor(ID.M.NumAmmoHeavy) < ammo0[0];
                boolean fired1 = zombies[1].getHealth() < 1000;
                boolean fired2 = zombies[2].getHealth() < 1000;
                check("stationary guard type 1, active AI: engages the noAI hostile in range", fired0, "zombieHp=" + zombies[0].getHealth());
                check("guard type 0: does not fire", !fired1, "zombieHp=" + zombies[1].getHealth());
                check("guard type 1 + PassiveAI: does not fire", !fired2, "zombieHp=" + zombies[2].getHealth());
                for (int i = 0; i < 3; i++) {
                    ships[i].setDead();
                    zombies[i].setDead();
                }
                return true;
            }
            return false;
        }
    }
}

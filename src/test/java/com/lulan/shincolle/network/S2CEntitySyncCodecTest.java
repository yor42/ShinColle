package com.lulan.shincolle.network;

import com.lulan.shincolle.entity.IShipOwner;
import com.lulan.shincolle.handler.ConfigHandler;
import com.lulan.shincolle.reference.unitclass.Attrs;
import com.lulan.shincolle.tileentity.TileEntitySmallShipyard;
import com.lulan.shincolle.utility.PacketHelper;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.EntityPig;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

import static com.lulan.shincolle.network.PacketTestSupport.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Issue #70: characterization of the S2CEntitySync codec (current wire format, ADR-0004).
 * <p>
 * Encode + decode round trips (buildable without a world, using stand-ins):
 * SyncShip_Riders 4, SyncEntity_PlayerUID 51, SyncEntity_PosRot 52, SyncEntity_Rot 53,
 * SyncEntity_Motion 54, SyncEntity_CustomData 56, SyncProjectile 80, SyncSystem_Config 81.
 * <p>
 * Decode-only (hand-written bytes, the reader width is pinned): every other ID. Their writers
 * cast the entity to BasicEntityShip / IShipEmotion and read the ship state arrays, attribute
 * data, inventory or buff map, so no encode seam exists without a ship instance:
 * SyncShip_AllMisc 0, Emo 1, Flag 2, Minor 3, Scale 5, Formation 6, Buffmap 7, Timer 8,
 * Guard 9, ID 10, UnitName 11, Attrs 12, PlayerSkillTimer 13, SyncEntity_Emo 50.
 * SyncEntity_Host 55 encodes only for EntityShipFishingHook (needs a world entity); the
 * generic-entity asymmetry is pinned below.
 * <p>
 * Known asymmetries (NET-08 / #44) that cannot be reproduced here without a ship: UnitName and
 * Buffmap write nothing for a null list/map but the reader expects a size.
 */
class S2CEntitySyncCodecTest {

    private static S2CEntitySync decode(S2CEntitySync msg) {
        return roundTrip(msg, S2CEntitySync::new);
    }

    private static byte[] bytes(ByteBuf buf) {
        byte[] b = new byte[buf.readableBytes()];
        buf.readBytes(b);
        return b;
    }

    /** Entity that also implements IShipOwner, for the PlayerUID packet. */
    static final class OwnerEntity extends Entity implements IShipOwner {
        OwnerEntity() {
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

        @Override
        public int getPlayerUID() {
            return 99;
        }

        @Override
        public void setPlayerUID(int uid) {
        }

        @Override
        public Entity getHostEntity() {
            return null;
        }
    }

    // ---------- round trips ----------

    @Test
    void projectile_id80_nullEntityWritesMinusOne() {
        S2CEntitySync msg = new S2CEntitySync((Entity) null, 5, S2CEntitySync.PID.SyncProjectile);

        ByteBuf expected = Unpooled.buffer();
        expected.writeByte(80).writeInt(-1).writeInt(5);
        assertArrayEquals(bytes(expected), encode(msg));

        S2CEntitySync d = decode(msg);
        assertEquals((byte) 80, (byte) field(d, "packetType"));
        assertEquals(-1, (int) field(d, "entityID"));
        assertEquals(5, (int) field(d, "valueInt"));
    }

    @Test
    void projectile_id80_withEntity() {
        bootstrap();
        S2CEntitySync d = decode(new S2CEntitySync(fakeEntity(4321, 0), 2, S2CEntitySync.PID.SyncProjectile));
        assertEquals(4321, (int) field(d, "entityID"));
        assertEquals(2, (int) field(d, "valueInt"));
    }

    @Test
    void customData_id56() {
        float[] data = {1F, 2.5F, -3F};
        S2CEntitySync msg = new S2CEntitySync((Entity) null, S2CEntitySync.PID.SyncEntity_CustomData, data);
        ByteBuf expected = Unpooled.buffer();
        expected.writeByte(56).writeInt(-1).writeInt(3).writeFloat(1F).writeFloat(2.5F).writeFloat(-3F);
        assertArrayEquals(bytes(expected), encode(msg));
        assertArrayEquals(data, field(decode(msg), "valueFloat1"));
    }

    @Test
    void customData_id56_nullArrayWritesMinusOneAndDecodesToEmpty() {
        S2CEntitySync msg = new S2CEntitySync((Entity) null, S2CEntitySync.PID.SyncEntity_CustomData, (float[]) null);
        ByteBuf expected = Unpooled.buffer();
        expected.writeByte(56).writeInt(-1).writeInt(-1);
        assertArrayEquals(bytes(expected), encode(msg));
        assertArrayEquals(new float[0], field(decode(msg), "valueFloat1"));
    }

    @Test
    void systemConfig_id81_sendsRingAbility() {
        int[] saved = ConfigHandler.ringAbility;
        try {
            ConfigHandler.ringAbility = new int[]{0, 6, 30, 20, 12};
            S2CEntitySync d = decode(new S2CEntitySync((Entity) null, S2CEntitySync.PID.SyncSystem_Config));
            assertArrayEquals(new int[]{0, 6, 30, 20, 12}, field(d, "valueInt1"));

            ConfigHandler.ringAbility = null;
            S2CEntitySync dn = decode(new S2CEntitySync((Entity) null, S2CEntitySync.PID.SyncSystem_Config));
            assertArrayEquals(new int[0], field(dn, "valueInt1"));
        } finally {
            ConfigHandler.ringAbility = saved;
        }
    }

    @Test
    void playerUID_id51_entityHost() {
        S2CEntitySync msg = new S2CEntitySync((Object) hostEntity(808), S2CEntitySync.PID.SyncEntity_PlayerUID);
        ByteBuf expected = Unpooled.buffer();
        expected.writeByte(51).writeInt(-1).writeInt(808).writeInt(-1).writeInt(-1).writeInt(99);
        assertArrayEquals(bytes(expected), encode(msg));
        assertArrayEquals(new int[]{808, -1, -1, 99}, field(decode(msg), "valueInt1"));
    }

    private static OwnerEntity hostEntity(int id) {
        bootstrap();
        OwnerEntity e = new OwnerEntity();
        e.setEntityId(id);
        return e;
    }

    @Test
    void playerUID_id51_tileHost() {
        bootstrap();
        TileEntitySmallShipyard t = new TileEntitySmallShipyard();
        t.setPos(new BlockPos(5, 6, 7));
        setField(t, "playerUID", 31);
        S2CEntitySync msg = new S2CEntitySync((Object) t, S2CEntitySync.PID.SyncEntity_PlayerUID);
        assertArrayEquals(new int[]{5, 6, 7, 31}, field(decode(msg), "valueInt1"));
    }

    @Test
    void playerUID_id51_nonOwnerHostWritesZeros() {
        S2CEntitySync msg = new S2CEntitySync((Object) "not an owner", S2CEntitySync.PID.SyncEntity_PlayerUID);
        ByteBuf expected = Unpooled.buffer();
        expected.writeByte(51).writeInt(-1).writeInt(0).writeInt(0).writeInt(0).writeInt(0);
        assertArrayEquals(bytes(expected), encode(msg));
        assertArrayEquals(new int[]{0, 0, 0, 0}, field(decode(msg), "valueInt1"));
    }

    @Test
    void posRot_id52_nonLivingEntity() {
        bootstrap();
        Entity e = fakeEntity(60, 0);
        e.posX = 1.5D;
        e.posY = 64.25D;
        e.posZ = -8D;
        e.rotationYaw = 90F;
        e.rotationPitch = -10F;

        S2CEntitySync msg = new S2CEntitySync(e, S2CEntitySync.PID.SyncEntity_PosRot);
        ByteBuf expected = Unpooled.buffer();
        expected.writeByte(52).writeInt(60).writeDouble(1.5D).writeDouble(64.25D).writeDouble(-8D);
        expected.writeFloat(90F).writeFloat(-10F).writeFloat(0F).writeFloat(0F);
        assertArrayEquals(bytes(expected), encode(msg));

        S2CEntitySync d = decode(msg);
        assertArrayEquals(new double[]{1.5D, 64.25D, -8D}, field(d, "valueDouble1"));
        assertArrayEquals(new float[]{90F, -10F, 0F, 0F}, field(d, "valueFloat1"));
    }

    @Test
    void posRot_id52_livingEntityAddsBodyAndHeadYaw() {
        EntityPig pig = livingStandIn(61);
        pig.posX = 2D;
        pig.posY = 3D;
        pig.posZ = 4D;
        pig.rotationYaw = 10F;
        pig.rotationPitch = 20F;
        pig.renderYawOffset = 30F;
        pig.rotationYawHead = 40F;

        S2CEntitySync d = decode(new S2CEntitySync(pig, S2CEntitySync.PID.SyncEntity_PosRot));
        assertArrayEquals(new double[]{2D, 3D, 4D}, field(d, "valueDouble1"));
        assertArrayEquals(new float[]{10F, 20F, 30F, 40F}, field(d, "valueFloat1"));
    }

    @Test
    void rot_id53_nonLivingRepeatsYaw() {
        bootstrap();
        Entity e = fakeEntity(62, 0);
        e.rotationYaw = 15F;
        e.rotationPitch = 5F;
        S2CEntitySync d = decode(new S2CEntitySync(e, S2CEntitySync.PID.SyncEntity_Rot));
        // non-living: [yaw, yaw, pitch]
        assertArrayEquals(new float[]{15F, 15F, 5F}, field(d, "valueFloat1"));
    }

    @Test
    void rot_id53_livingStartsWithHeadYaw() {
        EntityPig pig = livingStandIn(63);
        pig.rotationYaw = 15F;
        pig.rotationPitch = 5F;
        pig.rotationYawHead = 77F;
        S2CEntitySync d = decode(new S2CEntitySync(pig, S2CEntitySync.PID.SyncEntity_Rot));
        assertArrayEquals(new float[]{77F, 15F, 5F}, field(d, "valueFloat1"));
    }

    @Test
    void motion_id54_isNarrowedToFloat() {
        bootstrap();
        Entity e = fakeEntity(64, 0);
        e.motionX = 0.1D;
        e.motionY = -0.5D;
        e.motionZ = 2D;
        S2CEntitySync msg = new S2CEntitySync(e, S2CEntitySync.PID.SyncEntity_Motion);
        ByteBuf expected = Unpooled.buffer();
        expected.writeByte(54).writeInt(64).writeFloat((float) 0.1D).writeFloat(-0.5F).writeFloat(2F);
        assertArrayEquals(bytes(expected), encode(msg));
        assertArrayEquals(new float[]{(float) 0.1D, -0.5F, 2F}, field(decode(msg), "valueFloat1"));
    }

    @Test
    void riders_id4_noPassengers() {
        bootstrap();
        Entity e = fakeEntity(65, 0);
        S2CEntitySync msg = new S2CEntitySync(e, S2CEntitySync.PID.SyncShip_Riders);
        // [type][eid][riderCount=0][host marker 0][riding entity 0]
        ByteBuf expected = Unpooled.buffer();
        expected.writeByte(4).writeInt(65).writeInt(0).writeInt(0).writeInt(0);
        assertArrayEquals(bytes(expected), encode(msg));
        S2CEntitySync d = decode(msg);
        assertEquals(0, (int) field(d, "valueInt"));
        assertArrayEquals(new int[]{0, 0}, field(d, "valueInt1"));
    }

    @Test
    void riders_id4_withPassengers() {
        bootstrap();
        Entity mount = fakeEntity(66, 0);
        Entity r1 = fakeEntity(701, 0);
        Entity r2 = fakeEntity(702, 0);
        addPassengerField(mount, r1);
        addPassengerField(mount, r2);

        S2CEntitySync d = decode(new S2CEntitySync(mount, S2CEntitySync.PID.SyncShip_Riders));
        assertEquals(2, (int) field(d, "valueInt"));
        assertArrayEquals(new int[]{701, 702, 0, 0}, field(d, "valueInt1"));
    }

    @SuppressWarnings("unchecked")
    private static void addPassengerField(Entity mount, Entity rider) {
        try {
            Field f = Entity.class.getDeclaredField("riddenByEntities");
            f.setAccessible(true);
            ((List<Entity>) f.get(mount)).add(rider);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

    @Test
    void host_id55_genericEntityWritesNothingAfterTheId_currentBehavior() {
        bootstrap();
        S2CEntitySync msg = new S2CEntitySync(fakeEntity(88, 0), S2CEntitySync.PID.SyncEntity_Host);
        ByteBuf expected = Unpooled.buffer();
        expected.writeByte(55).writeInt(88);
        assertArrayEquals(bytes(expected), encode(msg));
    }

    @Disabled("NET-08 / #44: SyncEntity_Host writes the host id only for EntityShipFishingHook, but fromBytes always reads an int (underflow for any other entity)")
    @Test
    void host_id55_genericEntityRoundTrips_knownAsymmetry() {
        bootstrap();
        decode(new S2CEntitySync(fakeEntity(88, 0), S2CEntitySync.PID.SyncEntity_Host));
    }

    private static EntityPig livingStandIn(int id) {
        bootstrap();
        try {
            Field uf = Class.forName("sun.misc.Unsafe").getDeclaredField("theUnsafe");
            uf.setAccessible(true);
            Object unsafe = uf.get(null);
            EntityPig pig = (EntityPig) unsafe.getClass().getMethod("allocateInstance", Class.class).invoke(unsafe, EntityPig.class);
            pig.setEntityId(id);
            return pig;
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

    // ---------- decode-only: reader width of IDs whose writers need a ship ----------

    private static ByteBuf header(int id, int eid) {
        return Unpooled.buffer().writeByte(id).writeInt(eid);
    }

    private static void ints(ByteBuf b, int n) {
        for (int i = 0; i < n; i++) b.writeInt(i + 1);
    }

    private static void bools(ByteBuf b, int n) {
        for (int i = 0; i < n; i++) b.writeBoolean(i % 2 == 0);
    }

    private static S2CEntitySync read(ByteBuf b) {
        S2CEntitySync d = new S2CEntitySync();
        d.fromBytes(b);
        assertEquals(0, b.readableBytes(), "reader must consume exactly the bytes laid out for this id");
        return d;
    }

    @Test
    void decode_id0_allMisc_39ints_18bools_2stacks() {
        bootstrap();
        ByteBuf b = header(0, 5);
        ints(b, 39);
        bools(b, 18);
        PacketHelper.sendItemStack(b, new ItemStack(Items.APPLE, 3));
        PacketHelper.sendItemStack(b, ItemStack.EMPTY);
        S2CEntitySync d = read(b);
        assertEquals(39, ((int[]) field(d, "valueInt1")).length);
        assertEquals(18, ((boolean[]) field(d, "valueBoolean1")).length);
        net.minecraft.util.NonNullList<ItemStack> stacks = field(d, "stacks");
        assertEquals(3, stacks.get(0).getCount());
        assertTrue(stacks.get(1).isEmpty());
    }

    @Test
    void decode_id1_and_id50_emotion_6ints_2bools() {
        for (int id : new int[]{1, 50}) {
            ByteBuf b = header(id, 5);
            ints(b, 6);
            bools(b, 2);
            S2CEntitySync d = read(b);
            assertEquals(6, ((int[]) field(d, "valueInt1")).length);
            assertEquals(2, ((boolean[]) field(d, "valueBoolean1")).length);
        }
    }

    @Test
    void decode_id2_flags_18bools() {
        ByteBuf b = header(2, 5);
        bools(b, 18);
        assertEquals(18, ((boolean[]) field(read(b), "valueBoolean1")).length);
    }

    @Test
    void decode_id3_minor_30ints() {
        ByteBuf b = header(3, 5);
        ints(b, 30);
        assertEquals(30, ((int[]) field(read(b), "valueInt1")).length);
    }

    @Test
    void decode_id5_scale_and_id8_timer_1int() {
        for (int id : new int[]{5, 8}) {
            ByteBuf b = header(id, 5);
            b.writeInt(12);
            assertEquals(12, (int) field(read(b), "valueInt"));
        }
    }

    @Test
    void decode_id6_formation_7ints_1float() {
        ByteBuf b = header(6, 5);
        ints(b, 7);
        b.writeFloat(0.75F);
        S2CEntitySync d = read(b);
        assertEquals(7, ((int[]) field(d, "valueInt1")).length);
        assertArrayEquals(new float[]{0.75F}, field(d, "valueFloat1"));
    }

    @Test
    void decode_id9_guard_6ints_and_id10_shipId_3ints_and_id13_skillTimers_4ints() {
        int[][] idAndCount = {{9, 6}, {10, 3}, {13, 4}};
        for (int[] ic : idAndCount) {
            ByteBuf b = header(ic[0], 5);
            ints(b, ic[1]);
            assertEquals(ic[1], ((int[]) field(read(b), "valueInt1")).length);
        }
    }

    @Test
    void decode_id7_buffMap() {
        ByteBuf b = header(7, 5);
        b.writeInt(2).writeInt(1).writeInt(10).writeInt(2).writeInt(20);
        Map<Integer, Integer> map = field(read(b), "valueMap1");
        assertEquals(Map.of(1, 10, 2, 20), map);
    }

    @Test
    void decode_id11_unitNames() {
        ByteBuf b = header(11, 5);
        b.writeInt(2);
        ByteBufUtils.writeUTF8String(b, "A");
        ByteBufUtils.writeUTF8String(b, "B");
        assertEquals(List.of("A", "B"), field(read(b), "valueString1"));
    }

    @Test
    void decode_id12_attrs_allFlagsOff_and_allFlagsOn() {
        ByteBuf off = header(12, 5);
        for (int i = 0; i < 7; i++) off.writeBoolean(false);
        S2CEntitySync d = read(off);
        assertNull(field(d, "valueByte1"));
        assertNull(field(d, "valueFloat5"));

        ByteBuf on = header(12, 5);
        for (int i = 0; i < 7; i++) on.writeBoolean(true);
        on.writeInt(3).writeByte(1).writeByte(2).writeByte(3);                       // bonus (bytes)
        for (int i = 0; i < 4; i++) {                                              // raw, equip, morale, potion
            on.writeInt(2).writeFloat(1F).writeFloat(2F);
        }
        for (int i = 0; i < 2; i++) {                                              // formation, buffed (+ trailing float)
            on.writeInt(Attrs.AttrsLength);
            for (int k = 0; k < Attrs.AttrsLength; k++) on.writeFloat(k);
            on.writeFloat(0.5F);
        }
        S2CEntitySync dOn = read(on);
        assertArrayEquals(new byte[]{1, 2, 3}, field(dOn, "valueByte1"));
        float[] formation = field(dOn, "valueFloat5");
        assertEquals(Attrs.AttrsLength + 1, formation.length);
        assertEquals(0.5F, formation[Attrs.AttrsLength]);
    }

    @Test
    void decode_id12_attrsShortFormationArrayThrows_currentBehavior() {
        // NET-08: a formation array shorter than AttrsLength makes System.arraycopy throw in the reader
        ByteBuf b = header(12, 5);
        for (int i = 0; i < 5; i++) b.writeBoolean(false);
        b.writeBoolean(true).writeBoolean(false);
        b.writeInt(2).writeFloat(1F).writeFloat(2F).writeFloat(3F);
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> new S2CEntitySync().fromBytes(b));
    }
}

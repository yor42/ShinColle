package com.lulan.shincolle.network;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.WorldProviderSurface;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.function.Supplier;

/**
 * Shared helpers for the packet codec characterization tests (issue #70).
 * <p>
 * Message fields are private and the production classes expose no getters (and must not be
 * changed for this task), so decoded state is read back by reflection. Entities, tile entities
 * and worlds are minimal stand-ins: the codecs only call a handful of getters on them.
 */
final class PacketTestSupport {

    private PacketTestSupport() {
    }

    /**
     * Encode with toBytes into a fresh heap buffer, decode with fromBytes into a fresh
     * instance created by the no-arg constructor, and assert nothing was left unread.
     */
    static <T extends IMessage> T roundTrip(IMessage msg, Supplier<T> emptyFactory) {
        ByteBuf buf = Unpooled.buffer();
        msg.toBytes(buf);
        T decoded = emptyFactory.get();
        decoded.fromBytes(buf);
        if (buf.readableBytes() != 0) {
            throw new AssertionError("decoder left " + buf.readableBytes() + " unread byte(s)");
        }
        return decoded;
    }

    static byte[] encode(IMessage msg) {
        ByteBuf buf = Unpooled.buffer();
        msg.toBytes(buf);
        byte[] bytes = new byte[buf.readableBytes()];
        buf.readBytes(bytes);
        return bytes;
    }

    /** Read a (private) instance field of the message, searching the class hierarchy. */
    @SuppressWarnings("unchecked")
    static <V> V field(Object target, String name) {
        Class<?> c = target.getClass();
        while (c != null) {
            try {
                Field f = c.getDeclaredField(name);
                f.setAccessible(true);
                return (V) f.get(target);
            } catch (NoSuchFieldException e) {
                c = c.getSuperclass();
            } catch (IllegalAccessException e) {
                throw new AssertionError(e);
            }
        }
        throw new AssertionError("no field " + name + " on " + target.getClass());
    }

    /** Set a (private) instance field, searching the class hierarchy. */
    static void setField(Object target, String name, Object value) {
        Class<?> c = target.getClass();
        while (c != null) {
            try {
                Field f = c.getDeclaredField(name);
                f.setAccessible(true);
                f.set(target, value);
                return;
            } catch (NoSuchFieldException e) {
                c = c.getSuperclass();
            } catch (IllegalAccessException e) {
                throw new AssertionError(e);
            }
        }
        throw new AssertionError("no field " + name + " on " + target.getClass());
    }

    /** A world stand-in that only knows its dimension id (no Minecraft bootstrap, no constructor run). */
    static World fakeWorld(int dimension) {
        try {
            WorldProvider provider = (WorldProvider) unsafeAllocate(WorldProviderSurface.class);
            provider.setDimension(dimension);
            World world = (World) unsafeAllocate(WorldServer.class);
            Field f = World.class.getField("provider");
            if (Modifier.isFinal(f.getModifiers())) f.setAccessible(true);
            f.set(world, provider);
            return world;
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

    /** A bare entity with an entity id and a world; implements only the abstract hooks. */
    static Entity fakeEntity(int entityId, int dimension) {
        bootstrap();
        Entity e = new FakeEntity();
        e.setEntityId(entityId);
        e.world = fakeWorld(dimension);
        return e;
    }

    /** A server player stand-in (allocated without running any constructor); only id and world are set. */
    static net.minecraft.entity.player.EntityPlayer fakePlayer(int entityId, int dimension) {
        bootstrap();
        try {
            net.minecraft.entity.player.EntityPlayer p =
                    (net.minecraft.entity.player.EntityPlayer) unsafeAllocate(net.minecraft.entity.player.EntityPlayerMP.class);
            p.setEntityId(entityId);
            p.world = fakeWorld(dimension);
            return p;
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

    static TileEntity fakeTile(int dimension, int x, int y, int z) {
        bootstrap();
        TileEntity t = new FakeTile();
        t.setWorld(fakeWorld(dimension));
        t.setPos(new BlockPos(x, y, z));
        return t;
    }

    /** Vanilla registries must exist before Entity/TileEntity classes initialize (same as TestEnvironmentTest). */
    static void bootstrap() {
        net.minecraft.init.Bootstrap.register();
    }

    private static Object unsafeAllocate(Class<?> type) throws ReflectiveOperationException {
        Field uf = Class.forName("sun.misc.Unsafe").getDeclaredField("theUnsafe");
        uf.setAccessible(true);
        Object unsafe = uf.get(null);
        return unsafe.getClass().getMethod("allocateInstance", Class.class).invoke(unsafe, type);
    }

    static final class FakeEntity extends Entity {
        FakeEntity() {
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

    static final class FakeTile extends TileEntity {
    }
}

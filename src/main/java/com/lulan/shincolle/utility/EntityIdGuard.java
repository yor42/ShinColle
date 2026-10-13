package com.lulan.shincolle.utility;

/**
 * Decision seam for Minecraft entity-id lookups (issue #9, NET-13).
 * Applies to Minecraft entity ids only, not ship/player UIDs (those use 0/-1 as "none").
 */
public final class EntityIdGuard {

    /** Wire value meaning "no entity". 0 is a real entity id and must never be used for this. */
    public static final int NO_ENTITY = -1;

    private EntityIdGuard() {
    }

    public static boolean isLookupable(int entityId) {
        return entityId >= 0;
    }

    /** Encode an optional entity for a packet: null becomes {@link #NO_ENTITY}, otherwise its entity id. */
    public static <E> int encodeOptional(E entity, java.util.function.ToIntFunction<E> idOf) {
        return entity == null ? NO_ENTITY : idOf.applyAsInt(entity);
    }
}

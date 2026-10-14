package com.lulan.shincolle.utility;

/**
 * (#56) Minecraft-independent identity of the last applied ship chunk loader state: chunk x/z, chunkloader mode
 * and the ticket object (compared by reference). The chunk set to force is a pure function of (cx, cz, mode),
 * so applyChunkLoader can be skipped while the key is unchanged.
 */
public final class ChunkLoaderApplyKey {

    private final int cx;
    private final int cz;
    private final int mode;
    private final Object ticket;

    public ChunkLoaderApplyKey(int cx, int cz, int mode, Object ticket) {
        this.cx = cx;
        this.cz = cz;
        this.mode = mode;
        this.ticket = ticket;
    }

    /**
     * @return true if the last applied state (may be null = nothing applied yet) equals the given state
     */
    public static boolean isUnchanged(ChunkLoaderApplyKey last, int cx, int cz, int mode, Object ticket) {
        return last != null && last.cx == cx && last.cz == cz && last.mode == mode && last.ticket == ticket;
    }
}

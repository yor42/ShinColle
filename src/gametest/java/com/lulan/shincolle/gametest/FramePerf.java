package com.lulan.shincolle.gametest;

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.Arrays;

/**
 * Opt-in client frame sampler (-Dshincolle.gametest.renderperf=true). Dev-only, part of the gametest source set.
 * <p>
 * Per frame (one iteration of Minecraft.runGameLoop, which includes the client tick when one is due):
 * <ul>
 * <li>interval = System.nanoTime delta between two consecutive RenderTickEvent START events</li>
 * <li>cpu = RenderTickEvent END minus START of the same frame (scene submission only; excludes the buffer swap)</li>
 * <li>alloc = com.sun.management.ThreadMXBean.getThreadAllocatedBytes delta on the render thread between two START events</li>
 * </ul>
 * No GPU synchronisation is done: the numbers are CPU-side wall time, not GPU time.
 */
public class FramePerf {

    public static final boolean ENABLED = Boolean.getBoolean("shincolle.gametest.renderperf");

    private enum Mode { IDLE, WARMUP, RECORD, DONE }

    private final com.sun.management.ThreadMXBean bean = (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
    private Mode mode = Mode.IDLE;
    private long threadId = -1;

    private int warmupFrames;
    private int warmDone;
    private int target;
    private int n;
    private double[] interval;    //ms
    private double[] cpu;         //ms
    private double[] alloc;       //bytes

    private long lastNow;
    private long lastAlloc;
    private long startNanos;
    private double prevCpuMs;
    private boolean haveCpu;

    private long gcCountStart;
    private long gcMsStart;
    private long gcCountEnd;
    private long gcMsEnd;

    /** arms a window: warmupFrames frames are discarded, then frames frames are recorded */
    public void begin(int warmupFrames, int frames) {
        this.warmupFrames = warmupFrames;
        this.target = frames;
        this.warmDone = 0;
        this.n = 0;
        this.interval = new double[frames];
        this.cpu = new double[frames];
        this.alloc = new double[frames];
        this.lastNow = 0;
        this.haveCpu = false;
        this.mode = warmupFrames > 0 ? Mode.WARMUP : Mode.RECORD;
        if (mode == Mode.RECORD) markGcStart();
    }

    public boolean isDone() {
        return mode == Mode.DONE;
    }

    public void cancel() {
        mode = Mode.IDLE;
    }

    private void markGcStart() {
        long[] g = gc();
        gcCountStart = g[0];
        gcMsStart = g[1];
    }

    private static long[] gc() {
        long c = 0, t = 0;
        for (GarbageCollectorMXBean b : ManagementFactory.getGarbageCollectorMXBeans()) {
            if (b.getCollectionCount() > 0) c += b.getCollectionCount();
            if (b.getCollectionTime() > 0) t += b.getCollectionTime();
        }
        return new long[] {c, t};
    }

    @SubscribeEvent
    public void onRender(TickEvent.RenderTickEvent event) {
        if (mode == Mode.IDLE || mode == Mode.DONE) return;

        if (event.phase == TickEvent.Phase.START) {
            if (threadId < 0) threadId = Thread.currentThread().threadId();
            long now = System.nanoTime();
            long al = bean.getThreadAllocatedBytes(threadId);

            if (lastNow != 0 && haveCpu) {
                if (mode == Mode.WARMUP) {
                    if (++warmDone >= warmupFrames) {
                        mode = Mode.RECORD;
                        markGcStart();
                    }
                } else {
                    interval[n] = (now - lastNow) / 1.0e6;
                    cpu[n] = prevCpuMs;
                    alloc[n] = al - lastAlloc;
                    if (++n >= target) {
                        long[] g = gc();
                        gcCountEnd = g[0];
                        gcMsEnd = g[1];
                        mode = Mode.DONE;
                    }
                }
            }
            lastNow = now;
            lastAlloc = al;
            startNanos = now;
        } else {
            prevCpuMs = (System.nanoTime() - startNanos) / 1.0e6;
            haveCpu = true;
        }
    }

    private static double mean(double[] a) {
        double s = 0;
        for (double v : a) s += v;
        return s / a.length;
    }

    private static double pct(double[] sorted, double p) {
        int idx = (int) Math.ceil(p * sorted.length) - 1;
        return sorted[Math.max(0, Math.min(sorted.length - 1, idx))];
    }

    /** one-line summary of the finished window */
    public String summary() {
        double[] iv = Arrays.copyOf(interval, n);
        double[] cp = Arrays.copyOf(cpu, n);
        double[] al = Arrays.copyOf(alloc, n);
        double[] ivS = iv.clone();
        double[] cpS = cp.clone();
        double[] alS = al.clone();
        Arrays.sort(ivS);
        Arrays.sort(cpS);
        Arrays.sort(alS);
        double mi = mean(iv);
        return String.format("frames=%d | interval_ms mean=%.3f p50=%.3f p95=%.3f p99=%.3f max=%.3f (mean fps %.1f) | cpu_ms mean=%.3f p95=%.3f max=%.3f"
                        + " | alloc_KB/frame mean=%.1f p95=%.1f max=%.1f | gc collections=%d gc_ms=%d",
                n, mi, pct(ivS, 0.5), pct(ivS, 0.95), pct(ivS, 0.99), ivS[n - 1], 1000.0 / mi,
                mean(cp), pct(cpS, 0.95), cpS[n - 1],
                mean(al) / 1024.0, pct(alS, 0.95) / 1024.0, alS[n - 1] / 1024.0,
                gcCountEnd - gcCountStart, gcMsEnd - gcMsStart);
    }

    /** machine-friendly csv fields of the finished window: mean/p95 interval, mean/p95 cpu, mean/p95 alloc KB */
    public String csv() {
        double[] iv = Arrays.copyOf(interval, n);
        double[] cp = Arrays.copyOf(cpu, n);
        double[] al = Arrays.copyOf(alloc, n);
        double[] ivS = iv.clone();
        double[] cpS = cp.clone();
        double[] alS = al.clone();
        Arrays.sort(ivS);
        Arrays.sort(cpS);
        Arrays.sort(alS);
        return String.format("%.3f,%.3f,%.3f,%.3f,%.1f,%.1f", mean(iv), pct(ivS, 0.95), mean(cp), pct(cpS, 0.95), mean(al) / 1024.0, pct(alS, 0.95) / 1024.0);
    }
}

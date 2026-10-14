package com.lulan.shincolle.utility;

import java.util.Locale;

/**
 * (#66) Cached cooldown text for the skill icon HUD. Produces exactly
 * {@code String.format("%.1f", (float) ticks * 0.05F)} (default FORMAT locale), but formats each
 * tick value only once. The cache is dropped when the default FORMAT locale changes, so
 * locale-dependent output (for example "1,5") stays identical to the uncached call.
 * Client-thread use only.
 */
public final class CooldownText {

    private static final int MAX_CACHED = 4096;

    private static String[] cache = new String[128];
    private static Locale cacheLocale;

    private CooldownText() {
    }

    public static String format(int ticks) {
        if (ticks < 0 || ticks >= MAX_CACHED) {
            return String.format("%.1f", (float) ticks * 0.05F);
        }

        Locale locale = Locale.getDefault(Locale.Category.FORMAT);
        if (!locale.equals(cacheLocale)) {
            cache = new String[128];
            cacheLocale = locale;
        }

        if (ticks >= cache.length) {
            String[] bigger = new String[Math.min(MAX_CACHED, Math.max(ticks + 1, cache.length * 2))];
            System.arraycopy(cache, 0, bigger, 0, cache.length);
            cache = bigger;
        }

        String s = cache[ticks];
        if (s == null) {
            s = String.format("%.1f", (float) ticks * 0.05F);
            cache[ticks] = s;
        }
        return s;
    }
}

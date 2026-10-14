package com.lulan.shincolle.utility;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Issue #66: the cached cooldown text must equal the uncached
 * {@code String.format("%.1f", (float) ticks * 0.05F)} for every value and every locale.
 */
class CooldownTextTest {

    private Locale saved;

    @BeforeEach
    void save() {
        saved = Locale.getDefault(Locale.Category.FORMAT);
    }

    @AfterEach
    void restore() {
        Locale.setDefault(Locale.Category.FORMAT, saved);
    }

    private static String reference(int ticks) {
        return String.format("%.1f", (float) ticks * 0.05F);
    }

    @Test
    void matchesUncachedFormatAcrossRangeTwice() {
        Locale.setDefault(Locale.Category.FORMAT, Locale.US);
        for (int pass = 0; pass < 2; pass++) {
            for (int t = -5; t < 5000; t++) {
                assertEquals(reference(t), CooldownText.format(t), "ticks=" + t + " pass=" + pass);
            }
        }
    }

    @Test
    void knownValues() {
        Locale.setDefault(Locale.Category.FORMAT, Locale.US);
        assertEquals("0.0", CooldownText.format(0));
        assertEquals("1.5", CooldownText.format(30));
        assertEquals("2.0", CooldownText.format(40));
    }

    @Test
    void localeChangeInvalidatesCache() {
        Locale.setDefault(Locale.Category.FORMAT, Locale.US);
        assertEquals("1.5", CooldownText.format(30));

        Locale.setDefault(Locale.Category.FORMAT, Locale.GERMANY);
        assertEquals("1,5", CooldownText.format(30));
        assertEquals(reference(30), CooldownText.format(30));

        Locale.setDefault(Locale.Category.FORMAT, Locale.US);
        assertEquals("1.5", CooldownText.format(30));
    }
}

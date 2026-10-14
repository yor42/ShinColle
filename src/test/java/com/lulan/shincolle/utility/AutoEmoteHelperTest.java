package com.lulan.shincolle.utility;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Issue #32: auto emote on chat uses the strict matcher. ASCII words and digits must match as whole words only.
 * Emote ids: 1 = love, 6 = angry, 22 = no, 10 = spin (digit alias "10").
 */
class AutoEmoteHelperTest {

    @Test
    void wholeWordMatches() {
        assertEquals(6, AutoEmoteHelper.findEmote("I am so angry today"));
        assertEquals(1, AutoEmoteHelper.findEmote("LOVE you"));
        assertEquals(22, AutoEmoteHelper.findEmote("no way"));
    }

    @Test
    void wordInsideAnotherWordDoesNotMatch() {
        assertEquals(-1, AutoEmoteHelper.findEmote("xyzangryxyz"));
        assertEquals(-1, AutoEmoteHelper.findEmote("know"));
        assertEquals(-1, AutoEmoteHelper.findEmote("hello"));
        assertEquals(-1, AutoEmoteHelper.findEmote("version12"));
    }

    @Test
    void digitAliasMatchesOnlyAsAToken() {
        assertEquals(10, AutoEmoteHelper.findEmote("emote 10 please"));
        assertEquals(-1, AutoEmoteHelper.findEmote("x100y"));
    }

    @Test
    void blankAndNullMessagesMatchNothing() {
        assertEquals(-1, AutoEmoteHelper.findEmote(null));
        assertEquals(-1, AutoEmoteHelper.findEmote("   "));
    }

    /** CJK aliases are still substring matches (existing strict-matcher rule, flagged for the maintainer) */
    @Test
    void cjkAliasStillMatchesAsSubstring() {
        assertEquals(1, AutoEmoteHelper.findEmote("我爱你们"));
    }
}

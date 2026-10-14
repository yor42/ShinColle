package com.lulan.shincolle.utility;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Issue #52 (a): the per-class verdict cache gives the same answer as the uncached simple-name hashcode lookup,
 * including two classes sharing a simple name, and is correct again after the cache is cleared.
 */
class TargetHelperUnattackCacheTest {

    static class Zombie {
    }

    static class Skeleton {
    }

    static class Same {
    }

    // a second class whose simple name is identical to Same
    static class Other {
        static class Same {
        }
    }

    private static boolean uncached(Class<?> cls, Map<Integer, String> list) {
        return list.containsKey(cls.getSimpleName().hashCode());
    }

    @Test
    void cachedVerdictMatchesUncachedLookup() {
        Map<Integer, String> list = new HashMap<>();
        list.put("Zombie".hashCode(), "Zombie");
        Map<Class<?>, Boolean> cache = new HashMap<>();

        for (Class<?> c : new Class<?>[]{Zombie.class, Skeleton.class, Zombie.class, Skeleton.class}) {
            assertEquals(uncached(c, list), TargetHelper.isUnattackableClass(c, list, cache));
        }
        assertTrue(TargetHelper.isUnattackableClass(Zombie.class, list, cache));
        assertFalse(TargetHelper.isUnattackableClass(Skeleton.class, list, cache));
    }

    @Test
    void classesWithSameSimpleNameShareVerdict() {
        Map<Integer, String> list = new HashMap<>();
        list.put("Same".hashCode(), "Same");
        Map<Class<?>, Boolean> cache = new HashMap<>();

        assertTrue(TargetHelper.isUnattackableClass(Same.class, list, cache));
        assertTrue(TargetHelper.isUnattackableClass(Other.Same.class, list, cache));
    }

    @Test
    void staleCacheIsReplacedAfterClear() {
        Map<Integer, String> list = new HashMap<>();
        Map<Class<?>, Boolean> cache = new HashMap<>();
        assertFalse(TargetHelper.isUnattackableClass(Zombie.class, list, cache));

        list.put("Zombie".hashCode(), "Zombie");
        // not cleared: still the old verdict (this is why every list change must clear the cache)
        assertFalse(TargetHelper.isUnattackableClass(Zombie.class, list, cache));

        cache.clear();
        assertTrue(TargetHelper.isUnattackableClass(Zombie.class, list, cache));
    }
}

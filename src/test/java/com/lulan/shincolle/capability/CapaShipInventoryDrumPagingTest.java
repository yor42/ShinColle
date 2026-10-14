package com.lulan.shincolle.capability;

import com.lulan.shincolle.entity.BasicEntityShip;
import com.lulan.shincolle.entity.battleship.EntityBattleshipNGT;
import com.lulan.shincolle.reference.ID;
import net.minecraft.init.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Issue #42 (INV-05): the paged inventory size is {@code 24 + DrumState * 18} with no upper bound,
 * but the backing list has {@link CapaShipInventory#SlotMax} (60) slots.
 * <p>
 * The ship is allocated without running its constructor (the codec tests use the same Unsafe
 * technique) and only the StateMinor array is populated; CapaShipInventory itself is the real class.
 * Reachability (static trace, not exercised here): EquipCalc.calcEquipAttrs adds +1 DrumState per
 * drum equip in ANY of the 6 equip slots, and SlotShipInventory.isItemValid accepts any BasicEquip in
 * any equip slot, so 3..6 drums are reachable through the GUI. The Crane loops iterate
 * {@code [SLOTS_SHIPINV, getSizeInventoryPaged())} calling getStackInSlotWithPageCheck.
 */
class CapaShipInventoryDrumPagingTest {

    @BeforeAll
    static void bootstrapMinecraft() {
        Bootstrap.register();
    }

    private static CapaShipInventory inventoryOfShipWithDrums(int drums) {
        try {
            Field uf = Class.forName("sun.misc.Unsafe").getDeclaredField("theUnsafe");
            uf.setAccessible(true);
            Object unsafe = uf.get(null);
            BasicEntityShip ship = (BasicEntityShip) unsafe.getClass().getMethod("allocateInstance", Class.class)
                    .invoke(unsafe, EntityBattleshipNGT.class);
            Field minor = BasicEntityShip.class.getDeclaredField("StateMinor");
            minor.setAccessible(true);
            int[] state = new int[ID.M.DrumState + 1];
            state[ID.M.DrumState] = drums;
            minor.set(ship, state);
            return new CapaShipInventory(CapaShipInventory.SlotMax, ship);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

    @Test
    void twoDrumsFillExactlyTheBackingList() {
        CapaShipInventory inv = inventoryOfShipWithDrums(2);
        assertEquals(CapaShipInventory.SlotMax, inv.getSizeInventoryPaged());
        assertTrue(inv.getStackInSlotWithPageCheck(CapaShipInventory.SlotMax - 1).isEmpty());
    }

    @Test
    void currentlyReportsAPagedSizeLargerThanTheBackingListWithThreeDrums() {
        CapaShipInventory inv = inventoryOfShipWithDrums(3);

        // Suspected defect: size 78 against 60 backing slots. A future clamp to SlotPages - 1 pages flips this.
        assertEquals(24 + 3 * 18, inv.getSizeInventoryPaged());
        assertTrue(inv.getSizeInventoryPaged() > inv.getSlots());
    }

    @Test
    void currentlyThrowsWhenAPagedLoopReachesTheFirstSlotPastTheBackingList() {
        CapaShipInventory inv = inventoryOfShipWithDrums(3);

        // The Crane loop shape: for (i = SLOTS_SHIPINV; i < getSizeInventoryPaged(); i++) get...WithPageCheck(i)
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> {
            for (int i = 6; i < inv.getSizeInventoryPaged(); i++) {
                inv.getStackInSlotWithPageCheck(i);
            }
        });
        assertTrue(thrown.getMessage().startsWith("Slot 60 not in valid range"), thrown.getMessage());
    }

    @Test
    void currentlyThrowsOnSetWithPageCheckPastTheBackingListToo() {
        CapaShipInventory inv = inventoryOfShipWithDrums(6);
        assertEquals(24 + 6 * 18, inv.getSizeInventoryPaged());
        assertThrows(RuntimeException.class,
                () -> inv.setInventorySlotWithPageCheck(CapaShipInventory.SlotMax, net.minecraft.item.ItemStack.EMPTY));
    }
}

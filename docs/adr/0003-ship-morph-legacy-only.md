---
status: accepted
---
# Ship-morph is a legacy-track feature and stays out of the shared core

Ship-morph (a player transformed into a ship through the Metamorph mod) remains supported on the legacy track: its bugs are fixed there (e.g. NET-04, ATTR-06). It is out of scope for the Modern track, because Metamorph has no known NeoForge equivalent. Morph-specific logic (supply buttons, morph skills, morph hunger and level rules) therefore lives only in the version-specific layer and is never extracted into the shared core, even where the math looks reusable. Decided 2026-10-13 (decision P-8).

## Consequences

- When ship logic is shared with morph code (e.g. `decrSupplies`, Training Book level math), the shared core owns the ship rule. The morph layer calls it and adds its own differences.
- Metamorph stays an optional dependency. Absent-mod launches must work (MOD-01).

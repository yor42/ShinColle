---
status: accepted
---
# Save compatibility covers this fork's 1.12.2 releases only

Worlds saved by this fork's published 1.12.2 releases (`v1.12.2.1.x`) must load with no loss of ships, player data, or tile state. Worlds from upstream 1.10.2 or earlier are unsupported. Phase 2 found that no NBT key changed between the 1.10.2 baseline and HEAD (only Crane energy keys were added). Supporting 1.10.2 worlds, however, would need DataFixers and old-save fixtures that nobody has asked for. Decided 2026-10-13 (decision P-11).

## Consequences

- Fixes that touch NBT loading must treat absent keys as "keep defaults", never as zero (e.g. INV-01, Crane energy).
- A Ship Spawn Egg, ship, or `TeitokuExtProps` key may never be renamed or removed without a migration path and approval.
- Phase 3 needs a save fixture produced by a published fork release to test this.

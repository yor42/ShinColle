# ShinColle

Domain language for ShinColle, a Minecraft mod of collectible, combat-capable shipgirls and hostile Abyssal ships. It is being maintained on a 1.12.2 legacy track and ported to a NeoForge modern track. Use these terms in docs, issues, tests and code comments. Legacy identifiers, lang keys and registry IDs are never renamed to match this glossary.

`PROPOSED` means the English term is not yet agreed; confirm it with the maintainer and against the lang files before relying on it.

## Porting

**Legacy track**:
The Minecraft 1.12.2 implementation in this repo (branch `mc-1.12.2`), running on Cleanroom.

**Modern track**:
The future NeoForge implementation.

**Shared core**:
Minecraft-independent gameplay logic and content definitions usable by both tracks. It does not exist yet.

**Historical baseline commit**:
`8bca563`, the last upstream PinkaLulan 1.10.2 commit in this repository's history.

**Historical behavior**:
Behavior defined by the source at the historical baseline commit. Earlier versions (1.7.10) are consulted only where 1.10.2 is ambiguous.
_Avoid_: "original behavior", "how it used to play" (observed gameplay of released builds is a different thing)

**Current behavior**:
What the 1.12.2 code does now, verified by evidence.

**Desired behavior**:
Maintainer-approved target behavior.

**Feature status**:
One of CONFIRMED_WORKING, CONFIRMED_BUG, INCOMPLETE_PORT, UNKNOWN_INTENT, PLANNED_DEVIATION (see `docs/porting/feature-status.md`).

**Review gate**:
The maintainer approval point that ends a phase.

## Ships and players

**Shipgirl** (艦娘, kanmusu):
A collectible companion entity based on a Kantai Collection ship. Code and the porting docs usually say "ship".

**Abyssal** (深海棲艦, shinkaiseikan):
A hostile ship entity.
_Avoid_: enemy ship, mob ship

**Admiral** (提督, teitoku):
The player in their role as owner of shipgirls.
_Avoid_: Teitoku, Commander, Owner

**Ship-morph**:
A player transformed into a ship through the Metamorph mod integration. It is a legacy-track-only feature.
_Avoid_: morph player, ship player

**Guard mode**:
The state of a shipgirl holding a position or block and firing at targets in range.
_Avoid_: guard state, sentry

## Items and blocks

**Grudge** (怨念):
The resource item that shipgirls consume as fuel and that is used in construction.

**Training Book** (整備員訓練書, lang "Maintenance Personnel Training Book"):
A consumable that grants a shipgirl levels.

**Modernization Toolkit** (近代化改修ツール):
A consumable that adds a random bonus point to a shipgirl's attributes.
_Avoid_: modernization kit

**Repair Goddess** (深海ノ女神ノ怨念, lang "The Grudge of Abyssal Goddess"):
A consumable that revives a shipgirl or ship-morph player on a lethal hit.

**Marriage Ring**:
The ring that marries a shipgirl to her Admiral, raising her level cap (Baubles integration).
_Avoid_: wedding ring

**Drum**:
Any equipment that unlocks one extra shipgirl inventory page: Drum Canister, Pumps Station, Extreme Voltage Transformer, Redstone Charging Coil.

**Ship Spawn Egg**:
The item that creates a shipgirl. It is both the Shipyard's output and the item a sunk shipgirl drops, carrying part of her state.
_Avoid_: ship egg, spawn egg

**Small Shipyard** (lang "Abyssal Hydrothermal Vent"):
The single-block construction station.

**Large Shipyard** (lang "Hadal Hydrothermal Vortex"):
The multiblock construction station built from Heavy Grudge Lumps.

**Crane** (lang "Abyssal Crane"):
The block that loads and unloads shipgirl inventories, fluid and energy at a Waypoint.

**Waypoint** (lang "Abyssal Waypoint"):
A block marking a point on a shipgirl's patrol or transport route.

## Ships, equipment and combat (added for issue #71)

**Hime** (姫, lang "Princess"):
The boss-tier Abyssal class (`entity/hime`, lang e.g. "Destroyer Princess" for 駆逐棲姫). "Princess" comes from `en_us.lang`; docs use "Hime" (maintainer decision), not "Princess".

**Mount** (code `BasicEntityMount`):
A rideable or attachable companion entity of a shipgirl or Abyssal (lang names such as "Airbase", "Abyssal Aircraft Carrier", "Sanrensouhou-chan"). No agreed English category name; "Mount" is the agreed term.

**Rensouhou** (連装砲, lang "Rensouhou-chan"):
The small summoned cannon entity (`EntityRensouhou`; `EntityRensouhouS` is "Shinkei Rensouhou-chan"). Do not translate as "turret".

**Equipment** (code `Equip*`, lang group "Equip"):
Items a shipgirl carries that change attributes (cannons, ammo, armor, airplanes, drums). The lang file has no single English category name; "Equipment" is the agreed term.

**Formation** (陣形, lang "Formation"):
A team arrangement that gives movement and buffs (lang: Line Ahead, Double Line, Diamond, Echelon, Line Abreast). Needs 5 or 6 ships.

**Guard Point** (code `getGuardedPos`, state `GuardX/Y/Z/Dim/Type`):
The position or entity a shipgirl is told to hold (see Guard mode). The code says "guarded pos"; there is no lang string.

**Hostile Mob** (code `BasicEntityShipHostile`, lang `gui.shincolle:team.hostile`):
The code category of Abyssal entities that extend vanilla `EntityMob`. A separate term from **Abyssal** (maintainer decision), not an alias.

**Morph inventory** (code `GuiMorphInventory`, `ContainerMorphInventory`, `ID.Gui.MORPHINVENTORY`):
The GUI and container for the inventory of a Ship-morph player.

**CMB-01**:
Finding code (`docs/porting/gaps/2026-10-13-combat-equipment.md`): triple hit (x3) never triggers because of a duplicate condition. Decision P-7: fix after a characterization test.

**CMB-02**:
Finding code (same file): day and night damage modifier tables are swapped. Decision P-7: fix after a characterization test.

Further terms live in the lang files and the zh-TW `cfg-info.md`. Add each with its original form when it is first used in docs.

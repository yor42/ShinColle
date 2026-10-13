---
status: accepted
---
# The server is authoritative for every client request

Public and dedicated multiplayer servers are in scope. The server therefore validates every client-to-server packet. Upstream ShinColle trusted client-supplied player IDs, ownership, positions and array lengths (NET-01/02/03/06/07, INV-04), and the legacy code inherits that. Under this decision:

- The acting player is always the packet's sender, never an ID in the payload.
- The server checks ownership or team membership, reach, loaded chunks and bounds before mutating anything.
- OP-only actions re-check permission on the server.

The same model is the spec for the Modern track's payload handlers. Decided 2026-10-13 (decision P-9).

## Considered Options

- Trusted servers only (log the issues, fix later): rejected, because NET-02 lets any player rewrite any ship's level and owner, and the Modern track needs a designed authorization model anyway.

## Consequences

- Authorization rules become pure predicates (actor, target owner, distance, dimension) and are candidates for the shared core.
- Fixes change the wire protocol, so client and server versions must match. Saves are unaffected.

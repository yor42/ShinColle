# Historical Baseline Lineage (1.10.2 → 1.12.2 port)

- Date: 2026-10-13
- Phase: 2 (recorded here because Phase 1 left no historical record)
- Author: Claude Code
- Related issue/ADR: decision P-2 (`../decisions/README.md`)

## Scope
Identify which commit in this repository represents "upstream ShinColle 1.10" (decision P-2), and how the current 1.12.2 code descends from it. Only local git history was used; no external sources were fetched.

## Evidence
1. `git log --author=PinkaLulan` plus `git show <c>:build.gradle`: commits up to `8bca563` (2018-02-14, "Tweak some ship skills for metamorph.") build `minecraft.version = "1.10.2-12.18.3.2221"`, mod version `1.10.2.7.0`. The next PinkaLulan commit, `0aad501` (2018-02-18, "1.12.2 init commit"), starts a 1.12.2 rewrite that runs to `a576df7` (2018-09-02, "Rewrite attack methods…"; several messages say "NYC", not yet completed).
2. `git merge-base --is-ancestor`: neither `8bca563` nor `0aad501` is an ancestor of the other. Both are ancestors of `7b71860`.
3. `7b71860` (Theray070696, 2019-04-02) has git parent `a576df7`. It is followed by `698ef40` "Post 1.12.2 port".
4. Whitespace-insensitive diff size over `src/main/java` (`git diff --shortstat -w --ignore-cr-at-eol`):

   | From → to | Files | +lines | -lines |
   |---|---|---|---|
   | `8bca563` → `7b71860` | 302 | 2,466 | 5,255 |
   | `a576df7` → `7b71860` | 340 | 11,289 | 16,380 |
   | `7b71860` → HEAD | 461 | 26,779 | 36,885 |

5. Later contributors: Theray070696 (2019), MystiaLorelei (2019), Cappycot/Chris Wang (2019-2021), KCFindstr and iborvs (2020-2021), misaka10843 and sddsd2332 (2022-2023), yor42 (2023-2025).

## Confirmed findings
- **The historical baseline for gap analysis is `8bca563`**, the last upstream PinkaLulan 1.10.2 commit, available locally (`git show 8bca563:<path>`).
- Theray070696's port sits on top of PinkaLulan's unfinished 1.12.2 rewrite in git terms. **In content, though, it is a port of the 1.10.2 code** (Evidence 4: about 7.7k changed lines from `8bca563`, compared with about 27.7k from `a576df7`). PinkaLulan's 2018 handler refactor (`59bb3a2`, `09393e2`, `a576df7`) is therefore largely not part of the current code. This was not verified per file.
- Most of the change since the port (Evidence 4, third row) is reformatting, API migration, and fork features. The subsystem reports find that gameplay logic is largely unchanged from `8bca563`, apart from specific regressions they list.

## Assumptions
- Upstream releases for 1.10.2 were built from commits near `8bca563`. Release tags are not present in this repository.

## Unknowns
- Behavior of earlier 1.7.10 builds (source on Google Drive per README; not consulted).
- Whether any behavior from PinkaLulan's 1.12.2 WIP survived into `7b71860` beyond what the diffs show.

## Risks
- "Historical" in all Phase 2 records means **source at `8bca563`**, not observed 1.10.2 gameplay. Inherited bugs (for example CMB-01, CMB-02) were also historical behavior that players experienced.

## Recommended next actions
1. Optionally build `8bca563` in a separate 1.10.2 environment to capture runtime characterization (needs a ForgeGradle 2.x/Java 8 setup; this is a build-system task, so it needs approval).

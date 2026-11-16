# Decision Log

Significant decisions for this project. Newest at the bottom. Entries are never deleted.

---

## 001 — Multi-module Maven build with a pure-Java core
- **Date:** 2026-11-13
- **Status:** Accepted
- **Context:** The brief requires the game engine to be pure Java with no Spring, while the rest of the backend is Spring Boot. The previous plan enforced that only with ArchUnit rules inside a single Maven project (modular monolith).
- **Decision:** Split `fighters-program` into a multi-module Maven build. `fighters-program-core` is a dependency-free pure-Java jar (package `it.fightersprogram.engine`). `fighters-program-common` holds the shared error format and pagination. Each domain (catalog, collection, identity, economy, trading, matches) is a Spring library module with package `it.fightersprogram.<module>`. `fighters-program-application` is the only bootable module (entry point, configuration, Flyway migrations). Boundaries are enforced by the build and by ArchUnit tests in `application`.
- **Alternatives:** Modular monolith with ArchUnit-only boundaries (rejected: the boundary was not enforced by the compiler or the build); one Spring Boot service per module (rejected: no independent deployment need, overkill for the project).
- **Consequences:** The engine cannot accidentally depend on Spring and is testable in isolation; more pom files and module boilerplate to maintain; the runnable artifact lives in `application`, so the app is started with `-pl application -am`.

---

## 002 — Provisional base-mode rules and catalogs
- **Date:** 2026-11-16
- **Status:** Proposed
- **Context:** SWD_EN01 (#11) is still open with the DSG, but milestone v0.1.0 needs a playable match. Update: the 80 delivered Fighters are now bundled (Euroatlantica official file, the other four generated from the DSG roster); only the rule values stay provisional.
- **Decision:** Use the provisional values in `docs/rules-open-values.md`, read from `game.rules.*` and `game.deck.*`, and five placeholder catalog files with the official schema. Nothing is hardcoded: when the DSG decide, only `application.properties` and the JSON files change.
- **Alternatives:** Wait for the DSG (rejected: the milestone is already late and the whole match flow would stay untested); hardcode the values in the engine (rejected: every change would need code).
- **Consequences:** The game is playable end to end now; balance results are not meaningful until the official catalogs and values arrive; the decision becomes Accepted (or is superseded) when #11 is closed.

---

## 003 — Provisional starter collection on registration
- **Date:** 2026-11-16
- **Status:** Proposed
- **Context:** Players get cards only from packs (SWD_EC02) and the initial credit bonus (SWD_EC01), both in milestone v0.1.4. Without cards nobody can build a deck, so no match could be played in v0.1.0/v0.1.2.
- **Decision:** On `UserRegistered`, the collection module grants 2 copies of every Comune and 1 of every Rara card. It is switched on by `game.starter-collection.enabled` and lives in one class (`StarterCollection`).
- **Alternatives:** Seed users and collections with a Flyway migration (rejected: only works for fixed test users); wait for packs (rejected: blocks the whole match flow).
- **Consequences:** Every new player can build a valid deck at once (Epic and Legendary cards still need packs). When SWD_EC01/EC02 land, the team either removes the class or keeps it as a small welcome gift.

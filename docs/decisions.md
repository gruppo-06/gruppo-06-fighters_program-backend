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

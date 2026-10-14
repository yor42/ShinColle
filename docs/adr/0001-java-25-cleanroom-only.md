---
status: accepted
---
# Build for Java 25 and Cleanroom only

The old RetroFuturaGradle build stopped resolving, so on 2026-10-13 the project moved to the CleanroomModTemplate-mixin buildscript (Unimined, Gradle 9.8.0), which compiles to Java 25 bytecode and runs on the Cleanroom loader (Java 21+). We accepted the template default instead of keeping Java 8 bytecode via Jabel, so the legacy track no longer loads on plain Forge 1.12.2 or Java 8.

## Considered Options

- Java 8 bytecode via Jabel, running on Forge and Cleanroom: kept plain Forge users, but needed more deviation from the template and a Java 8 toolchain.
- Java 21 bytecode, Cleanroom only: a lower toolchain than the template, with no benefit the user wanted.

## Consequences

- Gradle must run on JDK 25.
- Players on plain Forge or Java 8 are unsupported.
- The build depends on the custom Unimined fork (`1.4.43-kappa`).
- Reverting to Java 8 bytecode would be a build-system change and needs approval.

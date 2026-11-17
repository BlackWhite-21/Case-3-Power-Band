---
applyTo: "src/game/**/*.java"
description: "Conventions for simple Java applications (Maven)"
---
- Use Java only. Target the JDK version declared in `pom.xml`.
- The entrypoint (`public static void main`) stays in `src/game/<package>/Main.java`.
- Domain classes stay in `src/game/<package>/model/`.
- Business logic stays in `src/game/<package>/service/`.
- Data access stays in `src/game/<package>/repository/`.
- Input and output types (requests, responses, DTOs) stay in `src/game/<package>/dto/`.
- Packages are lowercase; classes are `PascalCase`; methods and fields are `camelCase`; constants are `UPPER_SNAKE_CASE`.
- One public top-level type per file, and the file name matches the type name.
- Depend on interfaces for services and repositories; keep fields `private` and expose behavior through methods.
- Prefer the standard library. Add a dependency only when the standard library cannot do the job.
- Use JUnit for unit testing. Test classes stay in src/test/ and mirror the package structure of the main code.
- Constants must be declare in src/game/util/IConstants.java and be public static final and implement as an interface.
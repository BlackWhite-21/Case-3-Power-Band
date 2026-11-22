# Deploy

After you change this layer, update this file so the next run stays current.

## Build files
- Maven project, Java release 22, compiler plugin 3.13.0, exec plugin 3.5.0, and configured main class `powerband.Main`: `pom.xml`.
- JNA 5.17.0 is resolved through Maven; there is no project-owned native build or C++ toolchain.
- The configured main class source `src/main/java/powerband/Main.java` does not currently exist.
- No packaging pipeline, CI workflow, container, or deployment infrastructure is implemented.

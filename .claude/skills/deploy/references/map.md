# Deploy

After you change this layer, update this file so the next run stays current.

## Build files
- Maven project, Java release 25, compiler plugin 3.13.0, exec plugin 3.5.0, and configured main class `powerband.Main`: `pom.xml`.
- JNA 5.17.0 is resolved through Maven; there is no project-owned native build or C++ toolchain.
- The configured main class is implemented at `src/main/java/powerband/Main.java`; it creates the editor, starts available instruments, and registers all synth commands for playback.
- No packaging pipeline, CI workflow, container, or deployment infrastructure is implemented.

## Runtime documentation
- `readme.md` documents Java/Maven/SuperCollider, Windows x64 and Huion driver requirements, physical verification still pending, and test execution.

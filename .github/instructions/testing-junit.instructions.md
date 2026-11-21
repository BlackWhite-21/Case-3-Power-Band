---
applyTo: "src/test/java/**/*.java"
description: "JUnit 5 testing conventions for Java projects"
---
- Use JUnit 5 (`org.junit.jupiter`).
- Tests mirror the package of the class under test in `src/test/java/powerband/<package>/`.
- Test classes are named `<ClassName>Test.java`.
- Test method names describe behavior, e.g. `rejectsDuplicateName`.
- Shared test helpers (fake OSC transport, etc.) live in `src/test/java/powerband/helpers/`.
- Tests never need SuperCollider, a joystick, a microphone or a camera running: use fakes and `@TempDir`.
- Run tests with `mvn test`.

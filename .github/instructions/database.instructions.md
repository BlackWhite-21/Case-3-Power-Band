---
applyTo: "src/main/java/powerband/repository/**/*.java"
description: "Rules for binary recording files, disk search, and external sorting"
---
# Database

Before changing persistence, read `.claude/skills/database/references/map.md` and the approved database spec. The implementation is in `src/main/java/powerband/repository/`.

- Keep binary file access, fixed-size serialization, disk-based binary search, and Balanced Merge Sort in `src/main/java/powerband/repository/`.
- Use `IConstants` for file names, paths, record sizes, field widths, and sort parameters.
- Search and sort the recording index on disk; do not load or sort index records in memory.
- Preserve `recordings.list` insertion order.
- Close every stream and random-access file. Wrap I/O failures in `RecordingException` or a specific descendant.
- `BinaryRecordingRepository` owns `recordings.music`, `recordings.index`, and `recordings.list`; `BalancedMergeSort` sorts using disk tapes.

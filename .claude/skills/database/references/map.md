# Database

After you change this layer, update this file so the next run stays current.

Source: `src/main/java/powerband/repository`

## Schema
- `recordings.music`: fixed 96-byte header and command records; header positions and exclusive end positions are byte offsets.
- `recordings.index`: fixed 72-byte UTF-16 name plus header-position records, ordered by name.
- `recordings.list`: UTF-8 `name|date` rows appended in recording order.
- Index records are searched by random-access binary search and sorted with four disk tapes; no in-memory index collection is used.

## Classes
### RecordingRepository
Persistence contract for a recording lifecycle. `src/main/java/powerband/repository/RecordingRepository.java`
- `beginRecording(long pRecordedAtEpochMillis): void` — creates a provisional music header.
- `appendEvent(SynthEvent pEvent): void` — appends one fixed-size command record.
- `stopRecording(): void` — closes the current capture for naming.
- `finalizeRecording(String pName): RecordingInfo` — writes the final header, sorted index entry, and chronological list row.
- `containsName(String pName): boolean` — checks the disk index.
- `findHeaderPosition(String pName): long` — binary-searches the index or throws `RecordingNotFoundException`.

### BinaryRecordingRepository
Implements recording files, serialization, duplicate lookup, and transactional finalization. `src/main/java/powerband/repository/BinaryRecordingRepository.java`
- `BinaryRecordingRepository():` uses the default `data/` directory.
- `BinaryRecordingRepository(Path pDataDirectory):` uses an injected directory for tests.
- `beginRecording(long pRecordedAtEpochMillis): void` — writes the provisional header.
- `appendEvent(SynthEvent pEvent): void` — appends a fixed-width command.
- `stopRecording(): void` — closes the capture for naming.
- `finalizeRecording(String pName): RecordingInfo` — finalizes music, index, and list files.
- `containsName(String pName): boolean` — checks for a name with disk binary search.
- `findHeaderPosition(String pName): long` — returns the header offset or throws not-found.

### BalancedMergeSort
Package-private external balanced merge sorter using two input and two output tapes. `src/main/java/powerband/repository/BalancedMergeSort.java`

### RecordingFileFormat
Package-private fixed-width header, command, name, and index serialization. `src/main/java/powerband/repository/RecordingFileFormat.java`

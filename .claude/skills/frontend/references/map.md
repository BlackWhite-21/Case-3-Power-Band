# Frontend

After you change this layer, update this file so the next run stays current.

Source: `src/main/java/powerband/ui`

## Implemented
- Swing recording controls, pending-name retry, status feedback, and error dialogs in `RecordingEditorPanel.java`.
- Frame wrapper in `RecordingEditorFrame.java`.

## Classes
### RecordingEditorPanel
Swing controls for starting/stopping a recording and assigning its name. `src/main/java/powerband/ui/RecordingEditorPanel.java`
- `RecordingEditorPanel(RecordingService pRecordingService)` — creates the editor panel for a recording service.
- Adds `Cargar...`, `Play`, and `Stop` controls for chronological listing, loading, timed playback, and cancellation.

### RecordingEditorFrame
Window wrapper for the recording panel. `src/main/java/powerband/ui/RecordingEditorFrame.java`
- `RecordingEditorFrame(RecordingService pRecordingService)` — creates and sizes the editor window.

## Entrypoints
- `powerband.Main.main(String[] pArguments): void` in `src/main/java/powerband/Main.java` — creates and displays the recording editor before starting the available instruments.

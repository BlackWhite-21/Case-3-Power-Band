# Frontend

After you change this layer, update this file so the next run stays current.

Source: `src/main/java/powerband/ui`

## Implemented
- Three-page Swing editor (Editor, Grabadora, Reproductor), editable command timeline, recording/load/play/stop actions, asynchronous service calls, and copy-save flow in `RecordingEditorPanel.java`.
- Application window wrapper in `RecordingEditorFrame.java`.

## Classes
### RecordingEditorPanel
Three-page Swing editor with an editable timeline, recording controls, chronological recording selection, playback, and save-as-copy behavior. `src/main/java/powerband/ui/RecordingEditorPanel.java`
- `RecordingEditorPanel(RecordingService pRecordingService)` — creates the editor panel for a recording service.

### RecordingEditorFrame
Window wrapper for the recording panel. `src/main/java/powerband/ui/RecordingEditorFrame.java`
- `RecordingEditorFrame(RecordingService pRecordingService)` — creates and sizes the editor window.

## Entrypoints
- `powerband.Main.main(String[] pArguments): void` in `src/main/java/powerband/Main.java` — creates and displays the recording editor before starting the available instruments.

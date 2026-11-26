# Frontend

After you change this layer, update this file so the next run stays current.

Source: `src/main/java/powerband/ui`

## Implemented
- Three-page Swing editor (Editor, Grabadora, Reproductor), editable command timeline, asynchronous actions, and copy-save flow in `RecordingEditorPanel.java`.
- Recording start/stop and pause/resume appear only on Grabadora. Stopping asks whether to save; declining discards the provisional recording, while accepting prompts for a name.
- Loading, playback, and playback stop appear in Reproductor.
- The original loaded timeline uses bounded-buffer playback; edited timeline events play from the working copy.
- Window close is intercepted to confirm before discarding an active or unnamed recording; otherwise the application shutdown handler controls disposal.
- Application window wrapper in `RecordingEditorFrame.java`.

## Classes
### RecordingEditorPanel
Three-page Swing editor with an editable timeline, page-specific recording/player controls, a stop-to-save-or-discard prompt, chronological recording selection, playback, and save-as-copy behavior. `src/main/java/powerband/ui/RecordingEditorPanel.java`
- `RecordingEditorPanel(RecordingService pRecordingService)` — creates the editor panel for a recording service.
- `setJoystickStatus(String pStatus): void` — displays joystick availability or failure in the header.

### RecordingEditorFrame
Window wrapper for the recording panel. `src/main/java/powerband/ui/RecordingEditorFrame.java`
- `RecordingEditorFrame(RecordingService pRecordingService)` — creates and sizes the editor window.
- `setJoystickStatus(String pStatus): void` — forwards joystick state to the visible recording panel.

## Entrypoints
- `powerband.Main.main(String[] pArguments): void` in `src/main/java/powerband/Main.java` — creates and displays the recording editor before starting the available instruments.

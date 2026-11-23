# Backend

After you change this layer, update this file so the next run stays current.

Source: `src/main/java/powerband`

## Implemented
- OSC encoder, UDP transport/client, Bridge synth commands, Huion pointer-to-instrument adapter and mapper, microphone capture/analyzer, DTOs, exceptions, and constants in `src/main/java/powerband/`.
- `Main` composes the recording service, Swing editor, OSC client, Huion saxophone instrument, and default-microphone violin instrument.
- Recording event timing, name validation, and save coordination in `src/main/java/powerband/service/RecordingService.java`.
- Binary persistence is in `src/main/java/powerband/repository/`; the recording UI is in `src/main/java/powerband/ui/`.
- No joystick instrument exists yet.

## Entrypoints
- Maven sets `powerband.Main` in `pom.xml`; it opens the editor and starts the available instruments after the window becomes visible.

## Classes
### Main
Application entrypoint and dependency composition root. `src/main/java/powerband/Main.java`
- `main(String[] pArguments): void` — opens the editor and starts Huion, microphone, and SuperCollider initialization.

### Main.InstrumentStarter
Package-private startup action contract for a device instrument. `src/main/java/powerband/Main.java`
- `start(): void` — starts an instrument and may report a `DeviceException`.

### IOscTransport
OSC transport interface. `src/main/java/powerband/osc/IOscTransport.java`
- `send(byte[] pData): void` — sends an OSC packet.
- `receive(int pTimeoutMs): byte[]` — waits for one response packet.
- `close(): void` — closes the transport.

### UdpOscTransport
UDP implementation for scsynth. `src/main/java/powerband/osc/UdpOscTransport.java`
- `send(byte[] pData): void` — sends bytes to the configured host and port.
- `receive(int pTimeoutMs): byte[]` — receives one packet with the given timeout.
- `close(): void` — closes the UDP socket.

### OscMessageEncoder
Encodes OSC messages with padded strings and big-endian arguments. `src/main/java/powerband/osc/OscMessageEncoder.java`
- `encode(String pPath, Object... pArgs): byte[]` — encodes an OSC path and arguments.

### OscClient
Creates synth nodes, sends controls, and checks server status. `src/main/java/powerband/osc/OscClient.java`
- `ping(): void` — checks the scsynth `/status` response.
- `newSynth(String pDefName, Object... pControls): int` — creates a synth node and returns its node ID.
- `setControl(int pNodeId, String pName, int pValue): void` — sends an integer control update.
- `setControl(int pNodeId, String pName, float pValue): void` — updates a floating-point synth control.
- `close(): void` — closes the OSC transport.

### SynthCommand
Bridge implementor base and action dispatcher. `src/main/java/powerband/synth/SynthCommand.java`
- `getSynthId(): int` — returns the stable ID used by recorded events.
- `getSynthName(): String` — returns the SuperCollider SynthDef name.
- `stopAll(): void` — stops active notes when supported.
- `execute(SynthEvent pEvent): void` — dispatches PLAY, ON, or OFF to the synth command.

### PianoCommand
Legacy `classicPiano` implementation. `src/main/java/powerband/synth/PianoCommand.java`
- `getSynthId(): int` — returns the piano ID.
- `getSynthName(): String` — returns `classicPiano`.

### BassCommand
Legacy `stringBass` implementation. `src/main/java/powerband/synth/BassCommand.java`
- `getSynthId(): int` — returns the bass ID.
- `getSynthName(): String` — returns `stringBass`.

### OrganCommand
Sustained `organ70s` implementation with active node tracking. `src/main/java/powerband/synth/OrganCommand.java`
- `getSynthId(): int` — returns the organ ID.
- `getSynthName(): String` — returns `organ70s`.
- `stopAll(): void` — releases every active organ note.

### GateSynthCommand
Package-private base for gate-controlled saxophone and violin commands. `src/main/java/powerband/synth/GateSynthCommand.java`
- `stopAll(): void` — releases all active notes.

### SaxophoneCommand
Sustained saxophone Bridge implementation. `src/main/java/powerband/synth/SaxophoneCommand.java`
- `getSynthId(): int` — returns the saxophone ID.
- `getSynthName(): String` — returns `saxophone`.

### ViolinCommand
Sustained violin Bridge implementation. `src/main/java/powerband/synth/ViolinCommand.java`
- `getSynthId(): int` — returns the violin ID.
- `getSynthName(): String` — returns `violin`.

### SynthCommandRegistry
Routes synth events by ID. `src/main/java/powerband/synth/SynthCommandRegistry.java`
- `register(SynthCommand pCommand): void` — registers a command by synth ID.
- `get(int pSynthId): SynthCommand` — returns the command or throws `SynthException`.
- `execute(SynthEvent pEvent): void` — routes and executes an event.
- `nameOf(int pSynthId): String` — returns the registered synth name or numeric ID.
- `stopAll(): void` — requests all registered commands to stop active notes.

### SynthEvent
Immutable event shared by instruments, commands, monitoring, and recordings. `src/main/java/powerband/dto/SynthEvent.java`
- `withOffset(long pOffsetMs): SynthEvent` — returns a copy with a new recording offset.
- Record accessors: `offsetMs(): long`, `synthId(): int`, `action(): int`, `note(): int`, `durMs(): int`, `velocity(): float`, and `timbre1()`, `timbre2()`, `timbre3(): float`.

### JoystickState
Normalized joystick axes and button bit mask. `src/main/java/powerband/dto/JoystickState.java`
- `isPressed(int pButtonIndex): boolean` — checks the selected button bit.
- Record accessors: `x(): float`, `y(): float`, `z(): float`, and `buttons(): int`.

### TabletReading
Normalized Huion position and pressure. `src/main/java/powerband/dto/TabletReading.java`
- Record accessors: `x(): float`, `y(): float`, and `pressure(): float`.

### DatosTableta
Immutable Huion pointer reading in Swing logical coordinates. `src/main/java/powerband/dto/DatosTableta.java`
- Record accessors: `x(): double`, `y(): double`, `pressure(): float`, and `estado(): EstadoPluma`.

### EstadoPluma
Huion pen state enum. `src/main/java/powerband/dto/EstadoPluma.java`
- Values: `PRESIONADO`, `MOVIMIENTO`, `LIBERADO`, and `FUERA_SUPERFICIE`.

### RecordingInfo
Recording-list row model. `src/main/java/powerband/dto/RecordingInfo.java`
- `toString(): String` — formats the date and recording name for display.
- Record accessors: `name(): String` and `recordedAt(): String`.

### LoadedRecording
Loaded music header and playback bounds. `src/main/java/powerband/dto/LoadedRecording.java`
- Record accessors: `name(): String`, `headerPosition(): long`, `endOffset(): long`, and `recordedAtEpochMillis(): long`.

### PowerBandException
Checked root for project exceptions. `src/main/java/powerband/exception/PowerBandException.java`
- Constructors accept a message, or a message and cause.

### SynthException
OSC and SuperCollider failure. `src/main/java/powerband/exception/SynthException.java`
- Constructors accept a message, or a message and cause.

### DeviceException
Physical-device failure. `src/main/java/powerband/exception/DeviceException.java`
- Constructors accept a message, or a message and cause.

### RecordingException
Recording and persistence failure base. `src/main/java/powerband/exception/RecordingException.java`
- Constructors accept a message, or a message and cause.

### RecordingNotFoundException
Missing recording failure. `src/main/java/powerband/exception/RecordingNotFoundException.java`
- Constructors accept a message, or a message and cause.

### DuplicateNameException
Duplicate recording-name failure. `src/main/java/powerband/exception/DuplicateNameException.java`
- Constructors accept a message, or a message and cause.

### InvalidNameException
Invalid recording-name failure. `src/main/java/powerband/exception/InvalidNameException.java`
- Constructors accept a message, or a message and cause.

### IConstants
Shared OSC, synth, MIDI, device, persistence, playback, UI, and error constants. `src/main/java/powerband/util/IConstants.java`
- Contains new saxophone/violin IDs and Huion input constants; no methods are declared.

### Instrument
Bridge abstraction base that forwards synth events to its command and event listener. `src/main/java/powerband/instrument/Instrument.java`
- No public methods are declared.

### HuionInputSource
Driver integration boundary for normalized tablet readings. `src/main/java/powerband/instrument/HuionInputSource.java`
- `start(Consumer<TabletReading> pReadingConsumer, Consumer<DeviceException> pFailureConsumer): void` — starts publishing pen readings and asynchronous failures.
- `stop(): void` — stops the device source.

### TabletaHuionInputSource
Adapts Swing tablet coordinates and pressure to normalized Huion readings. `src/main/java/powerband/instrument/TabletaHuionInputSource.java`
- `TabletaHuionInputSource(Component pComponent)` — creates a JNA-backed tablet source for a Swing component.
- `start(Consumer<TabletReading> pReadingConsumer, Consumer<DeviceException> pFailureConsumer): void` — initializes capture and publishes normalized readings and failures.
- `stop(): void` — requests asynchronous tablet shutdown.

### HuionInputMapper
Maps normalized tablet readings to MIDI notes and note-on/off events. `src/main/java/powerband/instrument/HuionInputMapper.java`
- `map(TabletReading pReading): List<SynthEvent>` — maps one reading and updates active-note state.

### HuionInstrument
Connects a Huion source and mapper to a synth command. `src/main/java/powerband/instrument/HuionInstrument.java`
- Constructors accept the source, synth command, event listener, and optionally a device-failure listener.
- `start(): void` — subscribes to the input source.
- `stop(): void` — stops the source and releases the active note.
- `accept(TabletReading pReading): void` — dispatches events for one reading.

### TabletaGrafica
Asynchronous Swing-facing Huion pointer adapter with listener lifecycle and failure reporting. `src/main/java/powerband/instrument/TabletaGrafica.java`
- `initialize(Component pComponent): CompletableFuture<Void>` — attaches capture to the displayable component.
- `addListener(TabletaListener pListener): void` — registers a data/error listener.
- `removeListener(TabletaListener pListener): void` — removes a registered listener.
- `getFailure(): Optional<DeviceException>` — returns the asynchronous capture failure, if any.
- `closeAsync(): CompletableFuture<Void>` — requests capture termination and returns its completion.
- `close(): void` — requests asynchronous termination.

### TabletaListener
Receives tablet data and optional device failures. `src/main/java/powerband/instrument/TabletaListener.java`
- `onDatosTableta(DatosTableta pDatos): void` — receives a pen reading off the EDT.
- `onError(DeviceException pException): void` — receives an asynchronous capture error.

### JnaPointerInputSource
Package-private JNA implementation using a thread message hook and Windows Pointer API, with an injectable API for lifecycle tests. `src/main/java/powerband/instrument/JnaPointerInputSource.java`
- `attach(Component pComponent): void` — installs the hook for the Swing window on the EDT.
- `pollEvent(int pTimeoutMs): DatosTableta` — waits for queued pointer data and maps its coordinates/state.
- `requestStop(): void` — prevents further pointer events from being queued.
- `detach(): void` — removes the hook, waits for active callbacks, then releases the strongly held callback reference.

### PointerEventSource
Package-private injectable source contract used by the adapter. `src/main/java/powerband/instrument/PointerEventSource.java`
- `attach(Component pComponent): void` — initializes the event source.
- `pollEvent(int pTimeoutMs): DatosTableta` — returns the next reading or `null` on timeout.
- `requestStop(): void` — requests capture termination.
- `detach(): void` — releases source resources.

### PointerEventMapper
Package-private Windows-message and coordinate-to-DTO mapper. `src/main/java/powerband/instrument/PointerEventMapper.java`
- `map(NativePointerEvent pEvent): DatosTableta` — validates pressure/coordinates and creates the normalized reading.

### NativePointerEvent
Package-private immutable queue event with a Swing geometry snapshot. `src/main/java/powerband/instrument/NativePointerEvent.java`
- Record accessors expose message, pressure, contact, coordinate availability, and captured scale/bounds.

### MicrophoneAudioSource
Injectable PCM source abstraction. `src/main/java/powerband/instrument/MicrophoneAudioSource.java`
- `open(): void` — opens the audio input.
- `readFrame(): float[]` — returns a normalized mono PCM frame.
- `close(): void` — releases the audio input.

### DefaultMicrophoneAudioSource
Java Sound target-line capture from the system default input. `src/main/java/powerband/instrument/DefaultMicrophoneAudioSource.java`
- `open(): void` — opens and starts the default microphone line.
- `readFrame(): float[]` — captures and normalizes one frame.
- `close(): void` — closes the target line.

### MicrophonePitchAnalyzer
Estimates monophonic pitch and RMS velocity from PCM frames. `src/main/java/powerband/instrument/MicrophonePitchAnalyzer.java`
- `processFrame(float[] pSamples): List<SynthEvent>` — returns note-on, note-off, or velocity update events.
- `release(): List<SynthEvent>` — releases the active note once.

### MicrophoneInstrument
Captures frames and dispatches analyzed notes through the violin command. `src/main/java/powerband/instrument/MicrophoneInstrument.java`
- `start(): void` — opens input and starts the capture worker.
- `stop(): void` — closes input, waits for the worker, and releases active notes.

### RecordingService
Coordinates event capture, monotonic offsets, recording names, and repository finalization. `src/main/java/powerband/service/RecordingService.java`
- Constructors accept a `RecordingRepository`; a second overload injects monotonic and wall clocks.
- `startRecording(): void` — starts a capture and records wall-clock and monotonic start times.
- `onSynthEvent(SynthEvent pEvent): void` — forwards an event with its elapsed recording offset.
- `stopRecording(): void` — stops accepting events and leaves the capture pending for a name.
- `saveRecording(String pName): RecordingInfo` — validates uniqueness and finalizes the pending recording.
- `isRecording(): boolean` — reports whether event capture is active.
- `hasPendingName(): boolean` — reports whether a stopped capture still needs a name.
- `listRecordings(): List<RecordingInfo>` — reads the chronologically ordered recording list.
- `loadRecording(String pName): LoadedRecording` — resolves and validates a recording for playback.
- `setPlaybackListener(Consumer<PowerBandException> pListener): void` — registers asynchronous playback failures.
- `setPlaybackCompletionListener(Runnable pListener): void` — registers natural playback completion.
- `getLoadedRecording(): LoadedRecording` — returns the current loaded recording.
- `isPlaying(): boolean` — reports playback activity.
- `playLoadedRecording(): void` — starts timed buffered playback in a daemon thread.
- `stopPlayback(): void` — interrupts playback and stops active synth commands.

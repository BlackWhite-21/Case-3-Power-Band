# Backend

After you change this layer, update this file so the next run stays current.

Source: `src/main/java/powerband`

## Implemented
- OSC encoder, UDP transport/client, Bridge synth commands, Huion mapping boundary, microphone capture/analyzer, DTOs, exceptions, and constants in `src/main/java/powerband/`.
- No `Main`, recording service/repository, or Swing UI exists yet; Huion's driver-specific source must implement `HuionInputSource`.

## Entrypoints
- Maven sets `powerband.Main` in `pom.xml`; `src/main/java/powerband/Main.java` is currently missing.

## Classes
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
- `start(Consumer<TabletReading> pReadingConsumer): void` — starts publishing pen readings.
- `stop(): void` — stops the device source.

### HuionInputMapper
Maps normalized tablet readings to MIDI notes and note-on/off events. `src/main/java/powerband/instrument/HuionInputMapper.java`
- `map(TabletReading pReading): List<SynthEvent>` — maps one reading and updates active-note state.

### HuionInstrument
Connects a Huion source and mapper to a synth command. `src/main/java/powerband/instrument/HuionInstrument.java`
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
Package-private JNA implementation using a thread message hook and Windows Pointer API. `src/main/java/powerband/instrument/JnaPointerInputSource.java`
- `attach(Component pComponent): void` — installs the hook for the Swing window on the EDT.
- `pollEvent(int pTimeoutMs): DatosTableta` — waits for queued pointer data and maps its coordinates/state.
- `requestStop(): void` — prevents further pointer events from being queued.
- `detach(): void` — removes the hook and releases references.

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

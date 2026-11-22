# Backend

After you change this layer, update this file so the next run stays current.

Source: `src/main/java/powerband`

## Implemented
- OSC encoder, UDP transport, client, legacy piano/bass/organ commands, synth registry, DTOs, exceptions, and constants in `src/main/java/powerband/`.
- No `Main`, instrument implementation, service, recording repository, or Swing UI exists yet.
- Empty placeholders only: `instrument/HuionInputMapper.java`, `synth/SaxophoneCommand.java`, and `synth/ViolinCommand.java`.

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

### MotionReading
Legacy camera motion DTO; still present but no camera reader is implemented. `src/main/java/powerband/dto/MotionReading.java`
- Record accessors: `amount(): float`, `x(): float`, and `y(): float`.

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
- Includes legacy camera constants; no methods are declared.

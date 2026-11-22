# Testing

After you change this layer, update this file so the next run stays current.

Source: `src/test/java/powerband`

## Test classes
### OscMessageEncoderTest
OSC encoding tests. `src/test/java/powerband/osc/OscMessageEncoderTest.java`
- `messageLengthIsMultipleOfFour(): void` — checks OSC packet alignment.
- `encodesPathTypeTagsAndBigEndianInt(): void` — checks address, type tag, and integer encoding.
- `encodesFloatAsIeeeBits(): void` — checks float encoding.
- `rejectsUnsupportedType(): void` — checks rejection of unsupported argument types.

### SynthCommandTest
OSC command and registry tests. `src/test/java/powerband/synth/SynthCommandTest.java`
- `setUp(): void` — creates a fake transport and OSC client before each test.
- `pianoSendsSNewWithClassicPiano(): void` — checks piano synth creation and controls.
- `bassSendsSNewWithStringBass(): void` — checks bass synth creation.
- `organNoteOffSendsGateZero(): void` — checks organ note release.
- `organOffWithoutOnSendsNothing(): void` — checks harmless unmatched OFF.
- `pianoIgnoresNoteOffBecauseItStopsAlone(): void` — checks one-shot piano behavior.
- `registryRoutesEventBySynthId(): void` — checks event routing and unknown IDs.
- `pingFailsWhenServerDoesNotAnswer(): void` — checks timeout handling.
- `pingSucceedsWithStatusReply(): void` — checks successful server status response.

### FakeOscTransport
Captures packets for tests. `src/test/java/powerband/helpers/FakeOscTransport.java`
- `send(byte[] pData): void` — stores a sent packet.
- `receive(int pTimeoutMs): byte[]` — returns the configured reply or throws timeout.
- `setReply(String pReply): void` — configures an ASCII reply.
- `close(): void` — no-op fake close.
- `getSent(): List<byte[]>` — returns captured packets.
- `lastAsText(): String` — decodes the most recent packet for assertions.

## Empty placeholders
- `src/test/java/powerband/instrument/HuionInputMapperTest.java` is empty and contains no tests.

## Build files
- JUnit Jupiter 5.11.4 and Surefire 3.5.2 are configured in `pom.xml`.
- Run tests with `mvn test`.

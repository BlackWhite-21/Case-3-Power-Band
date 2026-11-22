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
- `saxophoneUpdatesAmplitudeWithoutRetriggeringAndStopsWithGate(): void` — checks sustained note and pressure update.
- `violinStartsAndStopsSustainedNote(): void` — checks violin gate behavior.
- `registryRoutesEventBySynthId(): void` — checks event routing and unknown IDs.
- `pingFailsWhenServerDoesNotAnswer(): void` — checks timeout handling.
- `pingSucceedsWithStatusReply(): void` — checks successful server status response.

### HuionInputMapperTest
Tablet-to-MIDI mapping tests. `src/test/java/powerband/instrument/HuionInputMapperTest.java`
- `mapsTabletCornersToConfiguredMidiRange(): void` — checks the minimum and maximum configured notes.
- `clampsCoordinatesAndPressureToTabletRange(): void` — checks normalization at out-of-range values.
- `emitsOffThenOnWhenPenMovesToAnotherNote(): void` — checks note switching while touching.
- `liftingPenReleasesOnlyTheCurrentlyActiveNote(): void` — checks one-time release on pen-up.

### HuionInstrumentTest
Bridge and input source integration test with a fake source. `src/test/java/powerband/instrument/HuionInstrumentTest.java`
- `dispatchesTabletEventsAndReleasesNoteWhenStopped(): void` — checks event and OSC dispatch without hardware.

### MicrophonePitchAnalyzerTest
PCM-to-violin pitch and velocity tests. `src/test/java/powerband/instrument/MicrophonePitchAnalyzerTest.java`
- `mapsA4ToneAndRmsToSustainedViolinEvent(): void` — checks pitch and level conversion.
- `silenceReleasesAnActiveNoteOnce(): void` — checks silence release behavior.
- `ignoresQuietInputUntilOnsetThresholdIsCrossed(): void` — checks the onset threshold.

### MicrophoneInstrumentTest
Asynchronous instrument test with a blocking fake audio source. `src/test/java/powerband/instrument/MicrophoneInstrumentTest.java`
- `usesInjectedAudioSourceAndDispatchesOnAndOffWithoutHardware(): void` — checks capture lifecycle and note events without a physical input.

### FakeOscTransport
Captures packets for tests. `src/test/java/powerband/helpers/FakeOscTransport.java`
- `send(byte[] pData): void` — stores a sent packet.
- `receive(int pTimeoutMs): byte[]` — returns the configured reply or throws timeout.
- `setReply(String pReply): void` — configures an ASCII reply.
- `close(): void` — no-op fake close.
- `getSent(): List<byte[]>` — returns captured packets.
- `lastAsText(): String` — decodes the most recent packet for assertions.

## Build files
- JUnit Jupiter 5.11.4 and Surefire 3.5.2 are configured in `pom.xml`.
- Run tests with `mvn test`.

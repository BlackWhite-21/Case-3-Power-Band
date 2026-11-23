# Testing

After you change this layer, update this file so the next run stays current.

Source: `src/test/java/powerband`

## Test classes
### MainTest
Application instrument-start isolation tests. `src/test/java/powerband/MainTest.java`
- `reportsOneInstrumentFailureWithoutPreventingAnotherFromStarting(): void` — verifies isolated device startup failures.
- `doesNotStartAnInstrumentAfterShutdownWasRequested(): void` — verifies shutdown prevents startup.

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
- `releasesActiveNoteAndReportsAsynchronousInputFailure(): void` — checks note release and device-error forwarding.

### TabletaHuionInputSourceTest
Swing tablet-to-instrument adapter tests with a fake pointer source. `src/test/java/powerband/instrument/TabletaHuionInputSourceTest.java`
- `normalizesCoordinatesAndReleasesPressureWhenPenLeavesContact(): void` — checks coordinate normalization and pen-up behavior off the EDT.
- `forwardsAsynchronousPointerFailures(): void` — checks pointer error delivery to the instrument boundary.

### PointerEventMapperTest
Huion Pointer API event, coordinate, pressure, and state mapping tests. `src/test/java/powerband/instrument/PointerEventMapperTest.java`
- `normalizesRealPressureAndConvertsHiDpiComponentCoordinates(): void` — checks pressure normalization and logical coordinates.
- `hoverMovementHasZeroPressureAndReleaseAlwaysClearsPressure(): void` — checks hover and leave pressure/state.
- `reportsUnsupportedPressureInsteadOfInventingAReading(): void` — rejects unavailable pressure.
- `reportsCoordinatesOutsideTheRegisteredComponentAsOutOfSurface(): void` — maps external coordinates to outside-surface state.
- `rejectsInvalidPressureAndCoordinateTransforms(): void` — rejects invalid pressure and scaling.

### TabletaGraficaTest
Asynchronous adapter lifecycle and listener tests using a fake pointer source. `src/test/java/powerband/instrument/TabletaGraficaTest.java`
- `dispatchesEventsOffTheEdtAndReleasesResourcesOnClose(): void` — checks worker delivery, release, and cleanup.
- `listenerFailureDoesNotPreventDeliveryToOtherListeners(): void` — checks listener isolation.
- `closingDoesNotBlockTheEventThreadWhileDetaching(): void` — checks nonblocking Swing shutdown.
- `reportsAsynchronousCaptureFailuresToListeners(): void` — checks capture-error reporting.
- `rejectsNonDisplayableComponentsAndRepeatedInitialization(): void` — checks lifecycle guards.
- `rejectsUnsupportedOsBeforeLoadingWindowsApi(): void` — checks unsupported-platform handling.

### JnaPointerStructureTest
Windows Pointer API structure layout tests without loading User32. `src/test/java/powerband/instrument/JnaPointerStructureTest.java`
- `nativeStructuresHaveConsistentFieldLayouts(): void` — checks JNA field orders, x64 structure sizes, and ABI offsets.

### JnaPointerInputSourceTest
Hook callback reference lifecycle tests with a fake Windows Pointer API. `src/test/java/powerband/instrument/JnaPointerInputSourceTest.java`
- `retainsCallbackUntilUnhookAndActiveCallbacksFinish(): void` — checks callback retention while hooks and callbacks are active.
- `retainsCallbackWhenUnhookFails(): void` — checks the callback remains retained after unhook failure.

### MicrophonePitchAnalyzerTest
PCM-to-violin pitch and velocity tests. `src/test/java/powerband/instrument/MicrophonePitchAnalyzerTest.java`
- `mapsA4ToneAndRmsToSustainedViolinEvent(): void` — checks pitch and level conversion.
- `silenceReleasesAnActiveNoteOnce(): void` — checks silence release behavior.
- `ignoresQuietInputUntilOnsetThresholdIsCrossed(): void` — checks the onset threshold.

### MicrophoneInstrumentTest
Asynchronous instrument test with a blocking fake audio source. `src/test/java/powerband/instrument/MicrophoneInstrumentTest.java`
- `usesInjectedAudioSourceAndDispatchesOnAndOffWithoutHardware(): void` — checks capture lifecycle and note events without a physical input.

### BinaryRecordingRepositoryTest
Binary files, search, index sorting, chronological list, and persistence-failure tests. `src/test/java/powerband/repository/BinaryRecordingRepositoryTest.java`
- `writesFixedSizeHeaderCommandsIndexAndChronologicalList(): void` — checks fixed record sizes, serialized fields, and file contents.
- `balancedMergeSortOrdersDiskIndexAndListKeepsRecordingOrder(): void` — checks disk-index order after each save and recording-order list.
- `readsAndAppendsEditedCopyWithoutChangingSourceRecording(): void` — checks bounded command reads, copy persistence, and source immutability.
- `duplicateCopyNameDoesNotAppendPartialMusic(): void` — checks duplicate rejection without a partial append.
- `duplicateNameKeepsStoppedRecordingAvailableForRetry(): void` — checks duplicate rejection and retry.
- `findsSavedRecordingAfterRepositoryIsReopened(): void` — checks persistent binary search with a new repository instance.
- `failedListWriteDoesNotReportSuccessAndKeepsRecordingAvailableForRetry(): void` — checks rollback and retry after a list-file I/O failure.
- `reportsMissingNamesAndRejectsBrokenMusicFile(): void` — checks not-found and I/O error reporting.

### RecordingServiceTest
Recording lifecycle, relative timing, name validation, edited copies, and playback tests with an in-memory repository fake. `src/test/java/powerband/service/RecordingServiceTest.java`
- `recordsMonotonicOffsetsInArrivalOrderAndIgnoresEventsAfterStop(): void` — checks event offsets and stop behavior.
- `rejectsStartingAnotherRecordingUntilThePendingOneIsSaved(): void` — checks the pending-name lifecycle.
- `rejectsInvalidAndDuplicateNamesButPreservesPendingRecordingForRetry(): void` — checks validation and retry behavior.
- `rejectsEmptyAndOverlongNames(): void` — checks name bounds.
- `rejectsStopAndSaveWithoutAnActiveOrPendingRecording(): void` — checks invalid lifecycle operations.
- `loadsEventsAndSavesEditedCopyWithoutChangingSource(): void` — checks loading and immutable-copy persistence.
- `keepsEditedEventsAvailableWhenCopyNameIsDuplicated(): void` — checks duplicate-name retry behavior.
- `rejectsUnorderedOrInvalidEditedEvents(): void` — checks edited-event validation.
- `playsEventsThroughSynthRegistryAndCompletesAfterDispatch(): void` — checks timed event dispatch through a fake OSC transport.

### RecordingEditorPanelTest
Headless Swing branding, page controls, and editable timeline tests. `src/test/java/powerband/ui/RecordingEditorPanelTest.java`
- `showsTheThreeEditorPagesAndPowerBandBranding(): void` — checks page navigation labels and logo color.
- `timelineEditsChangeOnlyTheWorkingCopy(): void` — checks editable fields and source-event immutability.

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

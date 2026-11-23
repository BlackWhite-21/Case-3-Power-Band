package powerband.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static powerband.util.IConstants.ACTION_ON;
import static powerband.util.IConstants.ACTION_PLAY;
import static powerband.util.IConstants.NANOS_PER_MILLI;
import static powerband.util.IConstants.SYNTH_ORGAN_ID;
import static powerband.util.IConstants.SYNTH_PIANO_ID;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import powerband.dto.RecordingInfo;
import powerband.dto.LoadedRecording;
import powerband.dto.SynthEvent;
import powerband.exception.DuplicateNameException;
import powerband.exception.InvalidNameException;
import powerband.exception.RecordingException;
import powerband.exception.RecordingNotFoundException;
import powerband.repository.RecordingRepository;
import powerband.helpers.FakeOscTransport;
import powerband.osc.OscClient;
import powerband.synth.PianoCommand;
import powerband.synth.SynthCommandRegistry;
import powerband.util.IConstants;

class RecordingServiceTest {

    private FakeRecordingRepository repository;
    private RecordingService service;
    private AtomicLong monotonicClock;
    private AtomicLong wallClock;

    @BeforeEach
    void setUp() {
        repository = new FakeRecordingRepository();
        monotonicClock = new AtomicLong(100L);
        wallClock = new AtomicLong(1_800_000_000_000L);
        service = new RecordingService(repository, monotonicClock::get, wallClock::get);
    }

    @Test
    void recordsMonotonicOffsetsInArrivalOrderAndIgnoresEventsAfterStop() throws Exception {
        service.startRecording();
        monotonicClock.addAndGet(25L * NANOS_PER_MILLI);
        service.onSynthEvent(event(48));
        monotonicClock.addAndGet(125L * NANOS_PER_MILLI);
        service.onSynthEvent(event(50));
        service.stopRecording();
        service.onSynthEvent(event(52));

        assertEquals(List.of(25L, 150L), repository.events.stream().map(SynthEvent::offsetMs).toList());
        assertEquals(List.of(48, 50), repository.events.stream().map(SynthEvent::note).toList());
        assertFalse(service.isRecording());
        assertTrue(service.hasPendingName());
    }

    @Test
    void pausesCaptureAndExcludesPausedTimeFromRecordedOffsets() throws Exception {
        service.startRecording();
        monotonicClock.addAndGet(20L * NANOS_PER_MILLI);
        service.onSynthEvent(event(48));

        service.pauseRecording();
        assertTrue(service.isPaused());
        monotonicClock.addAndGet(500L * NANOS_PER_MILLI);
        service.onSynthEvent(event(50));

        service.resumeRecording();
        assertFalse(service.isPaused());
        monotonicClock.addAndGet(30L * NANOS_PER_MILLI);
        service.onSynthEvent(event(52));

        service.pauseRecording();
        monotonicClock.addAndGet(70L * NANOS_PER_MILLI);
        service.stopRecording();

        assertEquals(List.of(20L, 50L), repository.events.stream().map(SynthEvent::offsetMs).toList());
        assertEquals(List.of(48, 52), repository.events.stream().map(SynthEvent::note).toList());
        assertFalse(service.isPaused());
        assertTrue(service.hasPendingName());
    }

    @Test
    void rejectsStartingAnotherRecordingUntilThePendingOneIsSaved() throws Exception {
        service.startRecording();
        assertThrows(RecordingException.class, service::startRecording);
        service.stopRecording();
        assertThrows(RecordingException.class, service::startRecording);
        service.saveRecording("primera");

        service.startRecording();
        assertTrue(service.isRecording());
    }

    @Test
    void rejectsInvalidAndDuplicateNamesButPreservesPendingRecordingForRetry() throws Exception {
        repository.names.add("ocupada");
        service.startRecording();
        service.stopRecording();

        assertThrows(InvalidNameException.class, () -> service.saveRecording("nombre/invalido"));
        assertThrows(DuplicateNameException.class, () -> service.saveRecording("ocupada"));
        assertTrue(service.hasPendingName());

        RecordingInfo pSaved = service.saveRecording("nueva");
        assertEquals("nueva", pSaved.name());
        assertFalse(service.hasPendingName());
        assertTrue(repository.names.contains("nueva"));
    }

    @Test
    void rejectsEmptyAndOverlongNames() throws Exception {
        service.startRecording();
        service.stopRecording();

        assertThrows(InvalidNameException.class, () -> service.saveRecording("   "));
        assertThrows(InvalidNameException.class, () -> service.saveRecording("n".repeat(33)));
        assertTrue(service.hasPendingName());
    }

    @Test
    void rejectsStopAndSaveWithoutAnActiveOrPendingRecording() {
        assertThrows(RecordingException.class, service::stopRecording);
        assertThrows(RecordingException.class, () -> service.saveRecording("sin-grabar"));
    }

    @Test
    void discardsActiveOrPendingRecordingAndAllowsStartingAgain() throws Exception {
        service.startRecording();
        service.onSynthEvent(event(48));

        service.discardRecording();

        assertFalse(service.isRecording());
        assertFalse(service.hasPendingName());
        assertTrue(repository.discarded);

        service.startRecording();
        service.stopRecording();
        service.discardRecording();

        assertFalse(service.isRecording());
        assertFalse(service.hasPendingName());
        assertEquals(2, repository.discardCount);
    }

    @Test
    void loadsEventsAndSavesEditedCopyWithoutChangingSource() throws Exception {
        SynthEvent pSourceEvent = event(48);
        repository.names.add("original");
        repository.recordings.put("original", List.of(pSourceEvent));
        List<SynthEvent> pLoadedEvents = service.loadRecording("original");
        SynthEvent pEditedEvent = new SynthEvent(125, SYNTH_ORGAN_ID, ACTION_ON,
                52, 200, 0.6f, 0.3f, 0.4f, 0.5f);

        RecordingInfo pCopy = service.saveEditedCopy("original", "copia", List.of(pEditedEvent));

        assertEquals("copia", pCopy.name());
        assertEquals(List.of(pSourceEvent), repository.recordings.get("original"));
        assertEquals(List.of(pEditedEvent), repository.recordings.get("copia"));
        assertEquals(List.of(pSourceEvent), pLoadedEvents);
    }

    @Test
    void keepsEditedEventsAvailableWhenCopyNameIsDuplicated() throws Exception {
        repository.names.add("original");
        repository.names.add("ocupada");
        repository.recordings.put("original", List.of(event(48)));
        List<SynthEvent> pEdits = List.of(event(52));

        assertThrows(
                DuplicateNameException.class,
                () -> service.saveEditedCopy("original", "ocupada", pEdits));
        assertEquals(List.of(event(52)), pEdits);
        assertEquals("nueva", service.saveEditedCopy("original", "nueva", pEdits).name());
    }

    @Test
    void rejectsUnorderedOrInvalidEditedEvents() throws Exception {
        repository.names.add("original");
        repository.recordings.put("original", List.of(event(48)));
        List<SynthEvent> pUnorderedEvents = List.of(
                event(48).withOffset(50),
                event(52).withOffset(20));

        assertThrows(
                RecordingException.class,
                () -> service.saveEditedCopy("original", "copia", pUnorderedEvents));
        assertFalse(repository.names.contains("copia"));
    }

    @Test
    void playsEventsThroughSynthRegistryAndCompletesAfterDispatch() throws Exception {
        FakeOscTransport pTransport = new FakeOscTransport();
        SynthCommandRegistry pRegistry = new SynthCommandRegistry();
        pRegistry.register(new PianoCommand(new OscClient(pTransport)));
        RecordingService pPlaybackService = new RecordingService(
                repository,
                pRegistry,
                monotonicClock::get,
                wallClock::get);
        List<SynthEvent> pEvents = List.of(
                new SynthEvent(0, SYNTH_PIANO_ID, ACTION_PLAY, 48, 10, 0.5f, 0f, 0f, 0f),
                new SynthEvent(5, SYNTH_PIANO_ID, ACTION_PLAY, 52, 10, 0.5f, 0f, 0f, 0f));

        pPlaybackService.playRecording(pEvents).get(2, TimeUnit.SECONDS);

        assertEquals(2, pTransport.getSent().size());
    }

    @Test
    void streamsLoadedRecordingThroughSynthRegistry() throws Exception {
        FakeOscTransport pTransport = new FakeOscTransport();
        SynthCommandRegistry pRegistry = new SynthCommandRegistry();
        pRegistry.register(new PianoCommand(new OscClient(pTransport)));
        repository.names.add("original");
        repository.recordings.put("original", List.of(
                new SynthEvent(0, SYNTH_PIANO_ID, ACTION_PLAY, 48, 10, 0.5f, 0f, 0f, 0f),
                new SynthEvent(0, SYNTH_PIANO_ID, ACTION_PLAY, 52, 10, 0.5f, 0f, 0f, 0f)));
        RecordingService pPlaybackService = new RecordingService(
                repository,
                pRegistry,
                monotonicClock::get,
                wallClock::get);

        pPlaybackService.loadRecording("original");
        pPlaybackService.playLoadedRecording().get(2, TimeUnit.SECONDS);

        assertEquals(2, pTransport.getSent().size());
        assertEquals("original", pPlaybackService.getLoadedRecording().name());
    }

    private SynthEvent event(int pNote) {
        return new SynthEvent(0L, SYNTH_ORGAN_ID, ACTION_ON, pNote, 0, 0.8f, 0.2f, 0.3f, 0.4f);
    }

    private static final class FakeRecordingRepository implements RecordingRepository {

        private final Set<String> names = new HashSet<>();
        private final List<SynthEvent> events = new ArrayList<>();
        private final Map<String, List<SynthEvent>> recordings = new java.util.HashMap<>();
        private List<SynthEvent> commandsForRead = List.of();
        private boolean stopped;
        private boolean discarded;
        private int discardCount;

        @Override
        public void beginRecording(long pRecordedAtEpochMillis) {
            events.clear();
            stopped = false;
            discarded = false;
        }

        @Override
        public void appendEvent(SynthEvent pEvent) throws RecordingException {
            if (stopped) {
                throw new RecordingException("No se aceptan eventos después de detener.");
            }
            events.add(pEvent);
        }

        @Override
        public void stopRecording() {
            stopped = true;
        }

        @Override
        public void discardRecording() {
            events.clear();
            stopped = true;
            discarded = true;
            discardCount++;
        }

        @Override
        public RecordingInfo finalizeRecording(String pName) {
            names.add(pName);
            recordings.put(pName, List.copyOf(events));
            return new RecordingInfo(pName, "2026-11-22 12:00:00");
        }

        @Override
        public boolean containsName(String pName) {
            return names.contains(pName);
        }

        @Override
        public long findHeaderPosition(String pName) throws RecordingNotFoundException {
            if (!names.contains(pName)) {
                throw new RecordingNotFoundException(pName);
            }
            return 0L;
        }

        @Override
        public List<RecordingInfo> listRecordings() {
            return names.stream()
                    .map(pName -> new RecordingInfo(pName, "2026-11-22 12:00:00"))
                    .toList();
        }

        @Override
        public List<SynthEvent> readEvents(String pName) throws RecordingNotFoundException {
            if (!recordings.containsKey(pName)) {
                throw new RecordingNotFoundException(pName);
            }
            return List.copyOf(recordings.get(pName));
        }

        @Override
        public LoadedRecording loadRecording(String pName) throws RecordingNotFoundException {
            if (!recordings.containsKey(pName)) {
                throw new RecordingNotFoundException(pName);
            }
            commandsForRead = List.copyOf(recordings.get(pName));
            long pEndOffset = (commandsForRead.size() + 1L) * IConstants.MUSIC_RECORD_SIZE;
            return new LoadedRecording(pName, 0L, pEndOffset, 0L);
        }

        @Override
        public List<SynthEvent> readCommands(long pStartOffset, long pEndOffset) {
            int pStartIndex = (int) (pStartOffset / IConstants.MUSIC_RECORD_SIZE) - 1;
            int pCount = Math.min(
                    IConstants.PLAYBACK_BUFFER_SIZE,
                    commandsForRead.size() - pStartIndex);
            if (pCount <= 0) {
                return List.of();
            }
            return List.copyOf(commandsForRead.subList(pStartIndex, pStartIndex + pCount));
        }

        @Override
        public RecordingInfo saveCopy(String pName, List<SynthEvent> pEvents, long pRecordedAtEpochMillis)
                throws RecordingException {
            if (names.contains(pName)) {
                throw new DuplicateNameException(pName);
            }
            names.add(pName);
            recordings.put(pName, List.copyOf(pEvents));
            return new RecordingInfo(pName, "2026-11-22 12:00:00");
        }
    }
}

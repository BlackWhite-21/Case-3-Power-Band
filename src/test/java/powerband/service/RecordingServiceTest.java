package powerband.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
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
<<<<<<< HEAD
=======
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
>>>>>>> 610fc0b32360ae0d30055cdda2e4bfc30343954a
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import powerband.dto.LoadedRecording;
import powerband.dto.RecordingInfo;
import powerband.dto.SynthEvent;
import powerband.exception.SynthException;
import powerband.exception.DuplicateNameException;
import powerband.exception.InvalidNameException;
import powerband.exception.RecordingException;
import powerband.exception.RecordingNotFoundException;
import powerband.repository.RecordingRepository;
import powerband.helpers.FakeOscTransport;
import powerband.osc.OscClient;
<<<<<<< HEAD
import powerband.synth.PianoCommand;
import powerband.synth.SynthCommandRegistry;
=======
import powerband.synth.SynthCommand;
import powerband.synth.SynthCommandRegistry;
import powerband.util.IConstants;
>>>>>>> 610fc0b32360ae0d30055cdda2e4bfc30343954a

class RecordingServiceTest {

    private final FakeRecordingRepository repository = new FakeRecordingRepository();
    private final AtomicLong monotonicClock = new AtomicLong(100L);
    private final AtomicLong wallClock = new AtomicLong(1_800_000_000_000L);
    private final RecordingService service =
            new RecordingService(repository, monotonicClock::get, wallClock::get);

    private static <T extends Throwable> void assertThrowsType(
            Class<T> pExceptionType,
            Executable pExecutable) {
        try {
            pExecutable.execute();
            fail("Se esperaba " + pExceptionType.getSimpleName());
        } catch (Throwable pThrown) {
            if (!pExceptionType.isInstance(pThrown)) {
                fail("Se esperaba " + pExceptionType.getSimpleName()
                        + " pero se lanzó " + pThrown.getClass().getSimpleName(),
                        pThrown);
            }
        }
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
        assertThrowsType(RecordingException.class, service::startRecording);
        service.stopRecording();
        assertThrowsType(RecordingException.class, service::startRecording);
        service.saveRecording("primera");

        service.startRecording();
        assertTrue(service.isRecording());
    }

    @Test
    void rejectsInvalidAndDuplicateNamesButPreservesPendingRecordingForRetry() throws Exception {
        repository.names.add("ocupada");
        service.startRecording();
        service.stopRecording();

        assertThrowsType(InvalidNameException.class, () -> service.saveRecording("nombre/invalido"));
        assertThrowsType(DuplicateNameException.class, () -> service.saveRecording("ocupada"));
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

        assertThrowsType(InvalidNameException.class, () -> service.saveRecording("   "));
        assertThrowsType(InvalidNameException.class, () -> service.saveRecording("n".repeat(33)));
        assertTrue(service.hasPendingName());
    }

    @Test
    void rejectsStopAndSaveWithoutAnActiveOrPendingRecording() {
        assertThrowsType(RecordingException.class, service::stopRecording);
        assertThrowsType(RecordingException.class, () -> service.saveRecording("sin-grabar"));
    }

    @Test
    void listsAndLoadsRecordingButRejectsPlaybackDuringCapture() throws Exception {
        repository.names.add("uno");

        assertEquals(1, service.listRecordings().size());
        assertEquals("uno", service.loadRecording("uno").name());

        service.startRecording();
        assertThrowsType(RecordingException.class, () -> service.loadRecording("uno"));
        assertThrowsType(RecordingException.class, service::playLoadedRecording);
    }

    @Test
    void playbackStopsPendingEvents() throws Exception {
        repository.names.add("ritmo");
        repository.playbackEvents = List.of(
                new SynthEvent(100L, SYNTH_ORGAN_ID, 1, 48, 0, 0.8f, 0.1f, 0.2f, 0.3f),
                new SynthEvent(150L, SYNTH_ORGAN_ID, 1, 50, 0, 0.8f, 0.1f, 0.2f, 0.3f));
        TestSynthCommand pCommand = new TestSynthCommand();
        SynthCommandRegistry pRegistry = new SynthCommandRegistry();
        pRegistry.register(pCommand);
        CountDownLatch pFirstWait = new CountDownLatch(1);
        CountDownLatch pAllowWait = new CountDownLatch(1);
        RecordingService pPlaybackService = new RecordingService(
                repository,
                monotonicClock::get,
                wallClock::get,
                pRegistry,
                pMillis -> {
                    pFirstWait.countDown();
                    pAllowWait.await();
                });

        pPlaybackService.loadRecording("ritmo");
        pPlaybackService.playLoadedRecording();
        assertTrue(pFirstWait.await(1, TimeUnit.SECONDS));
        pPlaybackService.stopPlayback();
        pAllowWait.countDown();

        assertFalse(pPlaybackService.isPlaying());
        assertTrue(pCommand.executed.isEmpty());
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

    private SynthEvent event(int pNote) {
        return new SynthEvent(0L, SYNTH_ORGAN_ID, ACTION_ON, pNote, 0, 0.8f, 0.2f, 0.3f, 0.4f);
    }

    private static final class TestSynthCommand extends SynthCommand {

        private final CopyOnWriteArrayList<SynthEvent> executed =
                new CopyOnWriteArrayList<>();

        private TestSynthCommand() {
            super(new OscClient(new FakeOscTransport()));
        }

        @Override
        public int getSynthId() {
            return SYNTH_ORGAN_ID;
        }

        @Override
        public String getSynthName() {
            return "testSynth";
        }

        @Override
        protected void play(SynthEvent pEvent) throws SynthException {
            executed.add(pEvent);
        }
    }

    private static final class FakeRecordingRepository implements RecordingRepository {

        private final Set<String> names = new HashSet<>();
        private final List<SynthEvent> events = new ArrayList<>();
<<<<<<< HEAD
        private final Map<String, List<SynthEvent>> recordings = new java.util.HashMap<>();
=======
        private List<SynthEvent> playbackEvents = List.of();
>>>>>>> 610fc0b32360ae0d30055cdda2e4bfc30343954a
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
<<<<<<< HEAD
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
        public RecordingInfo saveCopy(String pName, List<SynthEvent> pEvents, long pRecordedAtEpochMillis)
                throws RecordingException {
            if (names.contains(pName)) {
                throw new DuplicateNameException(pName);
            }
            names.add(pName);
            recordings.put(pName, List.copyOf(pEvents));
            return new RecordingInfo(pName, "2026-11-22 12:00:00");
=======
            List<RecordingInfo> pRecordings = new ArrayList<>();
            for (String pName : names) {
                pRecordings.add(new RecordingInfo(pName, "2026-11-22 12:00:00"));
            }
            return pRecordings;
        }

        @Override
        public LoadedRecording loadRecording(String pName)
                throws RecordingNotFoundException {
            if (!names.contains(pName)) {
                throw new RecordingNotFoundException(pName);
            }
            long pEndOffset = IConstants.MUSIC_RECORD_SIZE
                    + (long) playbackEvents.size() * IConstants.MUSIC_RECORD_SIZE;
            return new LoadedRecording(
                    pName,
                    0L,
                    pEndOffset,
                    1_800_000_000_000L);
        }

        @Override
        public List<SynthEvent> readCommands(long pStartOffset, long pEndOffset) {
            return playbackEvents;
>>>>>>> 610fc0b32360ae0d30055cdda2e4bfc30343954a
        }
    }
}

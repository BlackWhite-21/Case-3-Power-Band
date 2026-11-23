package powerband.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static powerband.util.IConstants.ACTION_ON;
import static powerband.util.IConstants.NANOS_PER_MILLI;
import static powerband.util.IConstants.SYNTH_ORGAN_ID;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
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
import powerband.synth.SynthCommand;
import powerband.synth.SynthCommandRegistry;
import powerband.util.IConstants;

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
        private List<SynthEvent> playbackEvents = List.of();
        private boolean stopped;

        @Override
        public void beginRecording(long pRecordedAtEpochMillis) {
            events.clear();
            stopped = false;
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
        public RecordingInfo finalizeRecording(String pName) {
            names.add(pName);
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
        }
    }
}

package powerband.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static powerband.util.IConstants.ACTION_ON;
import static powerband.util.IConstants.NANOS_PER_MILLI;
import static powerband.util.IConstants.SYNTH_ORGAN_ID;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import powerband.dto.RecordingInfo;
import powerband.dto.SynthEvent;
import powerband.exception.DuplicateNameException;
import powerband.exception.InvalidNameException;
import powerband.exception.RecordingException;
import powerband.exception.RecordingNotFoundException;
import powerband.repository.RecordingRepository;

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

    private SynthEvent event(int pNote) {
        return new SynthEvent(0L, SYNTH_ORGAN_ID, ACTION_ON, pNote, 0, 0.8f, 0.2f, 0.3f, 0.4f);
    }

    private static final class FakeRecordingRepository implements RecordingRepository {

        private final Set<String> names = new HashSet<>();
        private final List<SynthEvent> events = new ArrayList<>();
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
    }
}

package powerband.service;

import java.util.Objects;
import java.util.function.LongSupplier;

import powerband.dto.RecordingInfo;
import powerband.dto.SynthEvent;
import powerband.exception.DuplicateNameException;
import powerband.exception.InvalidNameException;
import powerband.exception.RecordingException;
import powerband.repository.RecordingRepository;
import powerband.util.IConstants;

public final class RecordingService {

    private final RecordingRepository repository;
    private final LongSupplier monotonicClock;
    private final LongSupplier wallClock;
    private boolean recording;
    private boolean pendingName;
    private long startedAtNanos;

    public RecordingService(RecordingRepository pRepository) {
        this(pRepository, System::nanoTime, System::currentTimeMillis);
    }

    public RecordingService(
            RecordingRepository pRepository,
            LongSupplier pMonotonicClock,
            LongSupplier pWallClock) {
        repository = Objects.requireNonNull(pRepository);
        monotonicClock = Objects.requireNonNull(pMonotonicClock);
        wallClock = Objects.requireNonNull(pWallClock);
    }

    public synchronized void startRecording() throws RecordingException {
        if (recording || pendingName) {
            throw new RecordingException(IConstants.ERR_ALREADY_RECORDING);
        }
        long pRecordedAt = wallClock.getAsLong();
        repository.beginRecording(pRecordedAt);
        startedAtNanos = monotonicClock.getAsLong();
        recording = true;
    }

    public synchronized void onSynthEvent(SynthEvent pEvent) throws RecordingException {
        if (!recording) {
            return;
        }
        long pElapsedNanos = Math.max(
                IConstants.ZERO_LONG,
                monotonicClock.getAsLong() - startedAtNanos);
        long pOffsetMs = pElapsedNanos / IConstants.NANOS_PER_MILLI;
        repository.appendEvent(Objects.requireNonNull(pEvent).withOffset(pOffsetMs));
    }

    public synchronized void stopRecording() throws RecordingException {
        if (!recording) {
            throw new RecordingException(IConstants.ERR_NOT_RECORDING);
        }
        repository.stopRecording();
        recording = false;
        pendingName = true;
    }

    public synchronized RecordingInfo saveRecording(String pName) throws RecordingException {
        if (!pendingName) {
            throw new RecordingException(IConstants.ERR_NOT_RECORDING);
        }
        String pValidatedName = validateName(pName);
        if (repository.containsName(pValidatedName)) {
            throw new DuplicateNameException(
                    String.format(IConstants.ERR_NAME_DUPLICATE, pValidatedName));
        }
        RecordingInfo pRecordingInfo = repository.finalizeRecording(pValidatedName);
        pendingName = false;
        return pRecordingInfo;
    }

    public synchronized boolean isRecording() {
        return recording;
    }

    public synchronized boolean hasPendingName() {
        return pendingName;
    }

    private String validateName(String pName) throws InvalidNameException {
        String pValue = pName == null ? IConstants.EMPTY_STRING : pName.strip();
        if (pValue.isEmpty()) {
            throw new InvalidNameException(IConstants.ERR_NAME_EMPTY);
        }
        if (pValue.length() > IConstants.NAME_LENGTH) {
            throw new InvalidNameException(IConstants.ERR_NAME_LONG);
        }
        if (!pValue.matches(IConstants.NAME_PATTERN)) {
            throw new InvalidNameException(IConstants.ERR_NAME_CHARS);
        }
        return pValue;
    }
}

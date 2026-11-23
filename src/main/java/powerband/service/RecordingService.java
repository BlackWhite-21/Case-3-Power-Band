package powerband.service;

import java.util.List;
import java.util.Objects;
<<<<<<< HEAD
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CancellationException;
import java.util.concurrent.locks.LockSupport;
=======
import java.util.function.Consumer;
>>>>>>> 610fc0b32360ae0d30055cdda2e4bfc30343954a
import java.util.function.LongSupplier;

import powerband.dto.LoadedRecording;
import powerband.dto.RecordingInfo;
import powerband.dto.SynthEvent;
import powerband.exception.DuplicateNameException;
import powerband.exception.InvalidNameException;
import powerband.exception.PowerBandException;
import powerband.exception.RecordingException;
import powerband.exception.RecordingNotFoundException;
<<<<<<< HEAD
import powerband.exception.SynthException;
=======
>>>>>>> 610fc0b32360ae0d30055cdda2e4bfc30343954a
import powerband.repository.RecordingRepository;
import powerband.synth.SynthCommandRegistry;
import powerband.util.IConstants;

public final class RecordingService {

    private final RecordingRepository repository;
    private final SynthCommandRegistry synthCommandRegistry;
    private final LongSupplier monotonicClock;
    private final LongSupplier wallClock;
    private final SynthCommandRegistry synthRegistry;
    private final PlaybackWaiter playbackWaiter;
    private boolean recording;
    private boolean paused;
    private boolean pendingName;
    private long startedAtNanos;
<<<<<<< HEAD
    private long pausedAtNanos;
    private long totalPausedNanos;
    private Thread playbackThread;

    public RecordingService(RecordingRepository pRepository) {
        this(pRepository, null, System::nanoTime, System::currentTimeMillis);
    }

    public RecordingService(RecordingRepository pRepository, SynthCommandRegistry pSynthCommandRegistry) {
        this(pRepository, pSynthCommandRegistry, System::nanoTime, System::currentTimeMillis);
=======
    private LoadedRecording loadedRecording;
    private volatile boolean playing;
    private volatile boolean playbackStopping;
    private volatile Thread playbackThread;
    private volatile Consumer<PowerBandException> playbackListener;
    private volatile Runnable playbackCompletionListener;

    public RecordingService(RecordingRepository pRepository) {
        this(
                pRepository,
                System::nanoTime,
                System::currentTimeMillis,
                new SynthCommandRegistry(),
                pMillis -> Thread.sleep(pMillis));
>>>>>>> 610fc0b32360ae0d30055cdda2e4bfc30343954a
    }

    public RecordingService(
            RecordingRepository pRepository,
            LongSupplier pMonotonicClock,
            LongSupplier pWallClock) {
<<<<<<< HEAD
        this(pRepository, null, pMonotonicClock, pWallClock);
=======
        this(
                pRepository,
                pMonotonicClock,
                pWallClock,
                new SynthCommandRegistry(),
                pMillis -> Thread.sleep(pMillis));
>>>>>>> 610fc0b32360ae0d30055cdda2e4bfc30343954a
    }

    public RecordingService(
            RecordingRepository pRepository,
<<<<<<< HEAD
            SynthCommandRegistry pSynthCommandRegistry,
            LongSupplier pMonotonicClock,
            LongSupplier pWallClock) {
=======
            LongSupplier pMonotonicClock,
            LongSupplier pWallClock,
            SynthCommandRegistry pSynthRegistry) {
        this(
                pRepository,
                pMonotonicClock,
                pWallClock,
                pSynthRegistry,
                pMillis -> Thread.sleep(pMillis));
    }

    public RecordingService(
            RecordingRepository pRepository,
            LongSupplier pMonotonicClock,
            LongSupplier pWallClock,
            SynthCommandRegistry pSynthRegistry,
            PlaybackWaiter pPlaybackWaiter) {
>>>>>>> 610fc0b32360ae0d30055cdda2e4bfc30343954a
        repository = Objects.requireNonNull(pRepository);
        synthCommandRegistry = pSynthCommandRegistry;
        monotonicClock = Objects.requireNonNull(pMonotonicClock);
        wallClock = Objects.requireNonNull(pWallClock);
        synthRegistry = Objects.requireNonNull(pSynthRegistry);
        playbackWaiter = Objects.requireNonNull(pPlaybackWaiter);
    }

    public synchronized void startRecording() throws RecordingException {
        if (recording || pendingName) {
            throw new RecordingException(IConstants.ERR_ALREADY_RECORDING);
        }
        long pRecordedAt = wallClock.getAsLong();
        repository.beginRecording(pRecordedAt);
        startedAtNanos = monotonicClock.getAsLong();
        pausedAtNanos = IConstants.ZERO_LONG;
        totalPausedNanos = IConstants.ZERO_LONG;
        paused = false;
        recording = true;
    }

    public synchronized void onSynthEvent(SynthEvent pEvent) throws RecordingException {
        if (!recording || paused) {
            return;
        }
        long pElapsedNanos = Math.max(
                IConstants.ZERO_LONG,
                monotonicClock.getAsLong() - startedAtNanos - totalPausedNanos);
        long pOffsetMs = pElapsedNanos / IConstants.NANOS_PER_MILLI;
        repository.appendEvent(Objects.requireNonNull(pEvent).withOffset(pOffsetMs));
    }

    public synchronized void stopRecording() throws RecordingException {
        if (!recording) {
            throw new RecordingException(IConstants.ERR_NOT_RECORDING);
        }
        long pNow = monotonicClock.getAsLong();
        repository.stopRecording();
        if (paused) {
            totalPausedNanos += Math.max(IConstants.ZERO_LONG, pNow - pausedAtNanos);
        }
        recording = false;
        paused = false;
        pendingName = true;
    }

    public synchronized void pauseRecording() throws RecordingException {
        if (!recording || paused) {
            throw new RecordingException(IConstants.ERR_NOT_RECORDING);
        }
        pausedAtNanos = monotonicClock.getAsLong();
        paused = true;
    }

    public synchronized void resumeRecording() throws RecordingException {
        if (!recording || !paused) {
            throw new RecordingException(IConstants.ERR_NOT_RECORDING);
        }
        totalPausedNanos += Math.max(
                IConstants.ZERO_LONG,
                monotonicClock.getAsLong() - pausedAtNanos);
        pausedAtNanos = IConstants.ZERO_LONG;
        paused = false;
    }

    public synchronized void discardRecording() throws RecordingException {
        if (!recording && !pendingName) {
            throw new RecordingException(IConstants.ERR_NOT_RECORDING);
        }
        repository.discardRecording();
        recording = false;
        paused = false;
        pendingName = false;
        startedAtNanos = IConstants.ZERO_LONG;
        pausedAtNanos = IConstants.ZERO_LONG;
        totalPausedNanos = IConstants.ZERO_LONG;
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

    public synchronized List<RecordingInfo> listRecordings() throws RecordingException {
        return repository.listRecordings();
    }

    public synchronized List<SynthEvent> loadRecording(String pName)
            throws RecordingException, RecordingNotFoundException {
        ensureNoRecordingOperation();
        return repository.readEvents(validateName(pName));
    }

    public synchronized RecordingInfo saveEditedCopy(
            String pSourceName,
            String pCopyName,
            List<SynthEvent> pEvents) throws RecordingException {
        ensureNoRecordingOperation();
        String pValidatedSourceName = validateName(pSourceName);
        String pValidatedCopyName = validateName(pCopyName);
        repository.findHeaderPosition(pValidatedSourceName);
        List<SynthEvent> pValidatedEvents = validateEvents(pEvents);
        return repository.saveCopy(pValidatedCopyName, pValidatedEvents, wallClock.getAsLong());
    }

    public synchronized CompletableFuture<Void> playRecording(List<SynthEvent> pEvents)
            throws RecordingException {
        ensureNoRecordingOperation();
        if (synthCommandRegistry == null) {
            throw new RecordingException(IConstants.ERR_PLAYBACK_UNAVAILABLE);
        }
        if (playbackThread != null && playbackThread.isAlive()) {
            throw new RecordingException(IConstants.ERR_PLAYBACK_ACTIVE);
        }
        List<SynthEvent> pValidatedEvents = validateEvents(pEvents);
        CompletableFuture<Void> pCompletion = new CompletableFuture<>();
        Thread pThread = Thread.ofVirtual()
                .name(IConstants.PLAYBACK_THREAD_NAME)
                .unstarted(() -> playEvents(pValidatedEvents, pCompletion));
        playbackThread = pThread;
        pThread.start();
        return pCompletion;
    }

    public synchronized void stopPlayback() {
        if (playbackThread != null) {
            playbackThread.interrupt();
        }
    }

    public synchronized boolean isPlaying() {
        return playbackThread != null && playbackThread.isAlive();
    }

    public String synthName(int pSynthId) {
        return synthCommandRegistry == null
                ? Integer.toString(pSynthId)
                : synthCommandRegistry.nameOf(pSynthId);
    }

    public synchronized boolean isRecording() {
        return recording;
    }

    public synchronized boolean isPaused() {
        return paused;
    }

    public synchronized boolean hasPendingName() {
        return pendingName;
    }

    public synchronized List<RecordingInfo> listRecordings() throws RecordingException {
        return repository.listRecordings();
    }

    public synchronized LoadedRecording loadRecording(String pName)
            throws RecordingException, RecordingNotFoundException {
        if (recording || pendingName) {
            throw new RecordingException(IConstants.ERR_PLAYBACK_LOAD_PENDING);
        }
        if (playing) {
            throw new RecordingException(IConstants.ERR_PLAYBACK_ACTIVE);
        }
        LoadedRecording pLoadedRecording = repository.loadRecording(pName);
        loadedRecording = pLoadedRecording;
        return pLoadedRecording;
    }

    public synchronized void setPlaybackListener(Consumer<PowerBandException> pListener) {
        playbackListener = Objects.requireNonNull(pListener);
    }

    public synchronized void setPlaybackCompletionListener(Runnable pListener) {
        playbackCompletionListener = Objects.requireNonNull(pListener);
    }

    public synchronized LoadedRecording getLoadedRecording() {
        return loadedRecording;
    }

    public synchronized boolean isPlaying() {
        return playing;
    }

    public synchronized void playLoadedRecording() throws RecordingException {
        if (recording || pendingName) {
            throw new RecordingException(IConstants.ERR_PLAYBACK_LOAD_PENDING);
        }
        if (playing) {
            throw new RecordingException(IConstants.ERR_PLAYBACK_ACTIVE);
        }
        LoadedRecording pLoadedRecording = loadedRecording;
        if (pLoadedRecording == null) {
            throw new RecordingException(IConstants.ERR_PLAYBACK_NOT_LOADED);
        }
        playbackStopping = false;
        playing = true;
        Thread pPlaybackThread = new Thread(
                () -> reproduce(pLoadedRecording),
                IConstants.PLAYBACK_THREAD_NAME);
        pPlaybackThread.setDaemon(true);
        playbackThread = pPlaybackThread;
        pPlaybackThread.start();
    }

    public synchronized void stopPlayback() throws RecordingException {
        if (!playing) {
            return;
        }
        playbackStopping = true;
        Thread pPlaybackThread = playbackThread;
        if (pPlaybackThread != null) {
            pPlaybackThread.interrupt();
        }
        if (pPlaybackThread != null && pPlaybackThread != Thread.currentThread()) {
            try {
                pPlaybackThread.join(IConstants.DEVICE_STOP_TIMEOUT_MS);
            } catch (InterruptedException pException) {
                Thread.currentThread().interrupt();
                throw new RecordingException(IConstants.ERR_PLAYBACK_STOP, pException);
            }
        }
        playing = false;
        playbackThread = null;
        synthRegistry.stopAll();
    }

    private void reproduce(LoadedRecording pLoadedRecording) {
        long pCursor = pLoadedRecording.headerPosition() + IConstants.MUSIC_RECORD_SIZE;
        long pPreviousOffset = IConstants.ZERO_LONG;
        try {
            while (!playbackStopping
                    && pCursor < pLoadedRecording.endOffset()) {
                List<SynthEvent> pEvents = repository.readCommands(
                        pCursor,
                        pLoadedRecording.endOffset());
                if (pEvents.isEmpty()) {
                    break;
                }
                for (SynthEvent pEvent : pEvents) {
                    if (playbackStopping || Thread.currentThread().isInterrupted()) {
                        return;
                    }
                    long pDelayMs = Math.max(
                            IConstants.ZERO_LONG,
                            pEvent.offsetMs() - pPreviousOffset);
                    if (pDelayMs > IConstants.ZERO_LONG) {
                        playbackWaiter.waitMs(pDelayMs);
                    }
                    if (playbackStopping || Thread.currentThread().isInterrupted()) {
                        return;
                    }
                    synthRegistry.execute(pEvent);
                    pPreviousOffset = pEvent.offsetMs();
                }
                pCursor += (long) pEvents.size() * IConstants.MUSIC_RECORD_SIZE;
            }
        } catch (InterruptedException pException) {
            Thread.currentThread().interrupt();
        } catch (PowerBandException pException) {
            notifyPlaybackFailure(pException);
        } finally {
            playing = false;
            playbackThread = null;
            synthRegistry.stopAll();
            notifyPlaybackCompletion();
        }
    }

    private void notifyPlaybackCompletion() {
        Runnable pListener = playbackCompletionListener;
        if (pListener != null) {
            pListener.run();
        }
    }

    private void notifyPlaybackFailure(PowerBandException pException) {
        Consumer<PowerBandException> pListener = playbackListener;
        if (pListener != null) {
            pListener.accept(pException);
        }
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

<<<<<<< HEAD
    private void ensureNoRecordingOperation() throws RecordingException {
        if (recording || pendingName) {
            throw new RecordingException(IConstants.ERR_RECORDING_BUSY);
        }
    }

    private List<SynthEvent> validateEvents(List<SynthEvent> pEvents) throws RecordingException {
        if (pEvents == null) {
            throw new RecordingException(IConstants.ERR_INVALID_EDITED_RECORDING);
        }
        List<SynthEvent> pValidatedEvents = new ArrayList<>(pEvents.size());
        long pPreviousOffset = IConstants.ZERO_LONG;
        for (SynthEvent pEvent : pEvents) {
            if (pEvent == null
                    || pEvent.offsetMs() < IConstants.ZERO_LONG
                    || pEvent.offsetMs() > IConstants.MAX_PLAYBACK_OFFSET_MS
                    || pEvent.offsetMs() < pPreviousOffset
                    || pEvent.note() < IConstants.MIDI_MIN_NOTE
                    || pEvent.note() > IConstants.MIDI_MAX_NOTE
                    || pEvent.durMs() < IConstants.ZERO_INT
                    || pEvent.synthId() <= IConstants.ZERO_INT
                    || pEvent.action() < IConstants.ACTION_PLAY
                    || pEvent.action() > IConstants.ACTION_OFF
                    || !isNormalized(pEvent.velocity())
                    || !isNormalized(pEvent.timbre1())
                    || !isNormalized(pEvent.timbre2())
                    || !isNormalized(pEvent.timbre3())) {
                throw new RecordingException(IConstants.ERR_INVALID_EDITED_RECORDING);
            }
            pPreviousOffset = pEvent.offsetMs();
            pValidatedEvents.add(pEvent);
        }
        return List.copyOf(pValidatedEvents);
    }

    private boolean isNormalized(float pValue) {
        return Float.isFinite(pValue)
                && pValue >= IConstants.HUION_PRESSURE_MIN
                && pValue <= IConstants.HUION_PRESSURE_MAX;
    }

    private void playEvents(List<SynthEvent> pEvents, CompletableFuture<Void> pCompletion) {
        Throwable pPlaybackFailure = null;
        try {
            long pPlaybackStart = System.nanoTime();
            for (SynthEvent pEvent : pEvents) {
                if (Thread.currentThread().isInterrupted()) {
                    throw new InterruptedException();
                }
                long pTargetDelay = Math.multiplyExact(pEvent.offsetMs(), IConstants.NANOS_PER_MILLI);
                waitUntil(pPlaybackStart, pTargetDelay);
                if (Thread.currentThread().isInterrupted()) {
                    throw new InterruptedException();
                }
                synthCommandRegistry.execute(pEvent);
            }
        } catch (InterruptedException pException) {
            Thread.currentThread().interrupt();
            pPlaybackFailure = new CancellationException(IConstants.LBL_PLAYBACK_STOPPED);
        } catch (ArithmeticException pException) {
            pPlaybackFailure = new RecordingException(IConstants.ERR_INVALID_EDITED_RECORDING, pException);
        } catch (SynthException pException) {
            pPlaybackFailure = new RecordingException(IConstants.ERR_SC_SEND, pException);
        } catch (RuntimeException pException) {
            pPlaybackFailure = new RecordingException(IConstants.ERR_UNKNOWN_RECORDING, pException);
        } finally {
            try {
                synthCommandRegistry.stopAll();
            } catch (RuntimeException pException) {
                RecordingException pCleanupFailure =
                        new RecordingException(IConstants.ERR_UNKNOWN_RECORDING, pException);
                if (pPlaybackFailure == null) {
                    pPlaybackFailure = pCleanupFailure;
                } else {
                    pPlaybackFailure.addSuppressed(pCleanupFailure);
                }
            } finally {
                synchronized (this) {
                    if (playbackThread == Thread.currentThread()) {
                        playbackThread = null;
                    }
                }
                if (pPlaybackFailure == null) {
                    pCompletion.complete(null);
                } else {
                    pCompletion.completeExceptionally(pPlaybackFailure);
                }
            }
        }
    }

    private void waitUntil(long pStartNanos, long pTargetDelayNanos) throws InterruptedException {
        long pRemainingNanos = pTargetDelayNanos - (System.nanoTime() - pStartNanos);
        while (pRemainingNanos > IConstants.ZERO_LONG) {
            LockSupport.parkNanos(pRemainingNanos);
            if (Thread.interrupted()) {
                throw new InterruptedException();
            }
            pRemainingNanos = pTargetDelayNanos - (System.nanoTime() - pStartNanos);
        }
=======
    @FunctionalInterface
    public interface PlaybackWaiter {

        void waitMs(long pMillis) throws InterruptedException;
>>>>>>> 610fc0b32360ae0d30055cdda2e4bfc30343954a
    }
}

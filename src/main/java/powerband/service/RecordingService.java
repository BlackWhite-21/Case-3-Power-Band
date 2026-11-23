package powerband.service;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.LongSupplier;

import powerband.dto.LoadedRecording;
import powerband.dto.RecordingInfo;
import powerband.dto.SynthEvent;
import powerband.exception.DuplicateNameException;
import powerband.exception.InvalidNameException;
import powerband.exception.PowerBandException;
import powerband.exception.RecordingException;
import powerband.exception.RecordingNotFoundException;
import powerband.repository.RecordingRepository;
import powerband.synth.SynthCommandRegistry;
import powerband.util.IConstants;

public final class RecordingService {

    private final RecordingRepository repository;
    private final LongSupplier monotonicClock;
    private final LongSupplier wallClock;
    private final SynthCommandRegistry synthRegistry;
    private final PlaybackWaiter playbackWaiter;
    private boolean recording;
    private boolean pendingName;
    private long startedAtNanos;
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
    }

    public RecordingService(
            RecordingRepository pRepository,
            LongSupplier pMonotonicClock,
            LongSupplier pWallClock) {
        this(
                pRepository,
                pMonotonicClock,
                pWallClock,
                new SynthCommandRegistry(),
                pMillis -> Thread.sleep(pMillis));
    }

    public RecordingService(
            RecordingRepository pRepository,
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
        repository = Objects.requireNonNull(pRepository);
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

    @FunctionalInterface
    public interface PlaybackWaiter {

        void waitMs(long pMillis) throws InterruptedException;
    }
}

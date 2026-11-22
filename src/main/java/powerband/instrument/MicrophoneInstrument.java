package powerband.instrument;

import static powerband.util.IConstants.DEVICE_STOP_TIMEOUT_MS;
import static powerband.util.IConstants.ERR_MIC_STOP;
import static powerband.util.IConstants.MICROPHONE_THREAD_NAME;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import powerband.dto.SynthEvent;
import powerband.exception.DeviceException;
import powerband.exception.PowerBandException;
import powerband.exception.SynthException;
import powerband.synth.SynthCommand;

/**
 * Captures default microphone frames and sends detected violin notes to its Bridge command.
 */
public class MicrophoneInstrument extends Instrument {

    private final MicrophoneAudioSource audioSource;
    private final MicrophonePitchAnalyzer analyzer;
    private final Consumer<PowerBandException> errorListener;
    private final AtomicBoolean running = new AtomicBoolean();
    private volatile Thread worker;

    public MicrophoneInstrument(MicrophoneAudioSource pAudioSource, SynthCommand pCommand,
            Consumer<SynthEvent> pEventListener, Consumer<PowerBandException> pErrorListener) {
        super(pCommand, pEventListener);
        this.audioSource = Objects.requireNonNull(pAudioSource);
        this.analyzer = new MicrophonePitchAnalyzer();
        this.errorListener = Objects.requireNonNull(pErrorListener);
    }

    public synchronized void start() throws DeviceException {
        if (running.get()) {
            return;
        }
        audioSource.open();
        running.set(true);
        worker = new Thread(this::capture, MICROPHONE_THREAD_NAME);
        worker.setDaemon(true);
        worker.start();
    }

    public void stop() throws DeviceException {
        running.set(false);
        DeviceException closeFailure = null;
        try {
            audioSource.close();
        } catch (DeviceException e) {
            closeFailure = e;
        }

        Thread activeWorker = worker;
        if (activeWorker != null && activeWorker != Thread.currentThread()) {
            try {
                activeWorker.join(DEVICE_STOP_TIMEOUT_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                DeviceException interrupted = new DeviceException(ERR_MIC_STOP, e);
                if (closeFailure != null) {
                    interrupted.addSuppressed(closeFailure);
                }
                throw interrupted;
            }
        }
        try {
            dispatchAnalyzerRelease();
        } catch (SynthException e) {
            DeviceException dispatchFailure = new DeviceException(ERR_MIC_STOP, e);
            if (closeFailure != null) {
                dispatchFailure.addSuppressed(closeFailure);
            }
            throw dispatchFailure;
        }
        if (closeFailure != null) {
            throw closeFailure;
        }
    }

    private void capture() {
        try {
            while (running.get()) {
                dispatchEvents(analyzer.processFrame(audioSource.readFrame()));
            }
        } catch (DeviceException | SynthException e) {
            running.set(false);
            errorListener.accept(e);
        } finally {
            try {
                audioSource.close();
            } catch (DeviceException e) {
                errorListener.accept(e);
            }
            try {
                dispatchAnalyzerRelease();
            } catch (SynthException e) {
                errorListener.accept(e);
            }
            worker = null;
        }
    }

    private void dispatchAnalyzerRelease() throws SynthException {
        dispatchEvents(analyzer.release());
    }

    private void dispatchEvents(List<SynthEvent> pEvents) throws SynthException {
        for (SynthEvent event : pEvents) {
            dispatch(event);
        }
    }
}

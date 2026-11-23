package powerband;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import powerband.dto.SynthEvent;
import powerband.exception.DeviceException;
import powerband.exception.PowerBandException;
import powerband.exception.RecordingException;
import powerband.exception.SynthException;
import powerband.instrument.DefaultMicrophoneAudioSource;
import powerband.instrument.HuionInstrument;
import powerband.instrument.MicrophoneInstrument;
import powerband.instrument.TabletaHuionInputSource;
import powerband.osc.OscClient;
import powerband.osc.UdpOscTransport;
import powerband.repository.BinaryRecordingRepository;
import powerband.service.RecordingService;
import powerband.synth.SaxophoneCommand;
import powerband.synth.ViolinCommand;
import powerband.synth.PianoCommand;
import powerband.synth.BassCommand;
import powerband.synth.OrganCommand;
import powerband.synth.SynthCommandRegistry;
import powerband.ui.RecordingEditorFrame;
import powerband.util.IConstants;

public final class Main {

    private static final System.Logger LOGGER = System.getLogger(Main.class.getName());

    private Main() {
    }

    public static void main(String[] pArguments) {
        SwingUtilities.invokeLater(Main::startApplication);
    }

    private static void startApplication() {
        OscClient pOscClient;
        try {
            pOscClient = new OscClient(new UdpOscTransport(
                    IConstants.SC_DEFAULT_HOST,
                    IConstants.SC_DEFAULT_PORT));
        } catch (IOException pException) {
            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(
                    null,
                    pException.getMessage(),
                    IConstants.MSG_ERROR_TITLE,
                    JOptionPane.ERROR_MESSAGE));
            return;
        }
        SynthCommandRegistry pSynthRegistry = new SynthCommandRegistry();
        pSynthRegistry.register(new PianoCommand(pOscClient));
        pSynthRegistry.register(new BassCommand(pOscClient));
        pSynthRegistry.register(new OrganCommand(pOscClient));
        pSynthRegistry.register(new SaxophoneCommand(pOscClient));
        pSynthRegistry.register(new ViolinCommand(pOscClient));
        RecordingService pRecordingService = new RecordingService(
                new BinaryRecordingRepository(),
                System::nanoTime,
                System::currentTimeMillis,
                pSynthRegistry);
        RecordingEditorFrame pFrame = new RecordingEditorFrame(pRecordingService);

        AtomicBoolean pShutdown = new AtomicBoolean();
        Object pInstrumentLifecycleLock = new Object();
        AtomicBoolean pRecordingFailureReported = new AtomicBoolean();
        Consumer<SynthEvent> pEventListener = pEvent -> recordEvent(
                pRecordingService,
                pRecordingFailureReported,
                pFrame,
                pEvent);
        HuionInstrument pHuionInstrument = new HuionInstrument(
                new TabletaHuionInputSource(pFrame.getContentPane()),
                new SaxophoneCommand(pOscClient),
                pEventListener,
                pException -> showError(pFrame, pException.getMessage()));
        MicrophoneInstrument pMicrophoneInstrument = new MicrophoneInstrument(
                new DefaultMicrophoneAudioSource(),
                new ViolinCommand(pOscClient),
                pEventListener,
                pException -> showError(pFrame, pException));

        pFrame.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent pEvent) {
                if (pShutdown.compareAndSet(false, true)) {
                    Thread.ofPlatform().start(() -> shutdown(
                            pHuionInstrument,
                            pMicrophoneInstrument,
                            pOscClient,
                            pInstrumentLifecycleLock));
                }
            }
        });
        pFrame.setVisible(true);

        Thread.startVirtualThread(() -> startInstrument(
                pHuionInstrument::start,
                pShutdown,
                pInstrumentLifecycleLock,
                pException -> showError(pFrame, pException.getMessage())));
        Thread.startVirtualThread(() -> startInstrument(
                pMicrophoneInstrument::start,
                pShutdown,
                pInstrumentLifecycleLock,
                pException -> showError(pFrame, pException)));
        Thread.startVirtualThread(() -> pingSuperCollider(pOscClient, pFrame, pShutdown));
    }

    static void startInstrument(
            InstrumentStarter pStarter,
            AtomicBoolean pShutdown,
            Object pInstrumentLifecycleLock,
            Consumer<DeviceException> pFailureListener) {
        synchronized (pInstrumentLifecycleLock) {
            if (pShutdown.get()) {
                return;
            }
            try {
                pStarter.start();
            } catch (DeviceException pException) {
                pFailureListener.accept(pException);
            }
        }
    }

    private static void pingSuperCollider(
            OscClient pOscClient,
            RecordingEditorFrame pFrame,
            AtomicBoolean pShutdown) {
        if (pShutdown.get()) {
            return;
        }
        try {
            pOscClient.ping();
        } catch (SynthException pException) {
            if (pShutdown.get()) {
                return;
            }
            String pMessage = String.format(
                    IConstants.MSG_SC_DOWN,
                    IConstants.SC_DEFAULT_HOST,
                    IConstants.SC_DEFAULT_PORT);
            LOGGER.log(System.Logger.Level.WARNING, pMessage, pException);
            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(
                    pFrame,
                    pMessage,
                    IConstants.MSG_ERROR_TITLE,
                    JOptionPane.WARNING_MESSAGE));
        }
    }

    private static void recordEvent(
            RecordingService pRecordingService,
            AtomicBoolean pFailureReported,
            RecordingEditorFrame pFrame,
            SynthEvent pEvent) {
        try {
            pRecordingService.onSynthEvent(pEvent);
        } catch (RecordingException pException) {
            LOGGER.log(System.Logger.Level.ERROR, pException.getMessage(), pException);
            if (pFailureReported.compareAndSet(false, true)) {
                showError(pFrame, pException.getMessage());
            }
        }
    }

    private static void shutdown(
            HuionInstrument pHuionInstrument,
            MicrophoneInstrument pMicrophoneInstrument,
            OscClient pOscClient,
            Object pInstrumentLifecycleLock) {
        synchronized (pInstrumentLifecycleLock) {
            try {
                try {
                    pHuionInstrument.stop();
                } catch (DeviceException pException) {
                    LOGGER.log(System.Logger.Level.ERROR, pException.getMessage(), pException);
                }
                try {
                    pMicrophoneInstrument.stop();
                } catch (DeviceException pException) {
                    LOGGER.log(System.Logger.Level.ERROR, pException.getMessage(), pException);
                }
            } finally {
                pOscClient.close();
            }
        }
    }

    private static void showError(RecordingEditorFrame pFrame, String pMessage) {
        String pSafeMessage = Objects.requireNonNullElse(pMessage, IConstants.ERR_UNKNOWN_APPLICATION);
        SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(
                pFrame,
                pSafeMessage,
                IConstants.MSG_ERROR_TITLE,
                JOptionPane.ERROR_MESSAGE));
    }

    private static void showError(RecordingEditorFrame pFrame, PowerBandException pException) {
        showError(pFrame, pException.getMessage());
    }

    @FunctionalInterface
    interface InstrumentStarter {

        void start() throws DeviceException;
    }
}

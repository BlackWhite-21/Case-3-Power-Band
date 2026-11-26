package powerband.instrument;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import powerband.dto.JoystickState;
import powerband.dto.SynthEvent;
import powerband.exception.DeviceException;
import powerband.exception.SynthException;
import powerband.synth.SynthCommand;
import powerband.util.IConstants;

public final class JoystickInstrument extends Instrument {

    private final JoystickInputSource inputSource;
    private final Consumer<DeviceException> failureListener;
    private final Consumer<JoystickState> statusListener;
    private final AtomicBoolean running = new AtomicBoolean();
    private volatile Thread worker;
    private int activeButtons;
    private JoystickState lastState;

    public JoystickInstrument(
            JoystickInputSource pInputSource,
            SynthCommand pCommand,
            Consumer<SynthEvent> pEventListener) {
        this(
                pInputSource,
                pCommand,
                pEventListener,
                pException -> {
                },
                pState -> {
                });
    }

    public JoystickInstrument(
            JoystickInputSource pInputSource,
            SynthCommand pCommand,
            Consumer<SynthEvent> pEventListener,
            Consumer<DeviceException> pFailureListener,
            Consumer<JoystickState> pStatusListener) {
        super(pCommand, pEventListener);
        inputSource = Objects.requireNonNull(pInputSource);
        failureListener = Objects.requireNonNull(pFailureListener);
        statusListener = Objects.requireNonNull(pStatusListener);
    }

    public synchronized void start() throws DeviceException {
        if (running.get()) {
            return;
        }
        inputSource.open();
        running.set(true);
        worker = new Thread(this::poll, IConstants.JOY_DEVICE_NAME);
        worker.setDaemon(true);
        worker.start();
    }

    public synchronized void stop() throws DeviceException {
        running.set(false);
        Thread pWorker = worker;
        if (pWorker != null && pWorker != Thread.currentThread()) {
            pWorker.interrupt();
            try {
                pWorker.join(IConstants.DEVICE_STOP_TIMEOUT_MS);
            } catch (InterruptedException pException) {
                Thread.currentThread().interrupt();
                throw new DeviceException(IConstants.ERR_JOY_READ, pException);
            }
        }
        inputSource.close();
        releaseActiveButtons();
        worker = null;
    }

    public void accept(JoystickState pState) {
        Objects.requireNonNull(pState);
        statusListener.accept(pState);
        boolean pAxesChanged = axesChanged(pState);
        int pPreviouslyActiveButtons = activeButtons;
        for (int pButtonIndex = IConstants.ZERO_INT;
                pButtonIndex < IConstants.JOY_BUTTON_COUNT;
                pButtonIndex++) {
            boolean pPressed = pState.isPressed(pButtonIndex);
            boolean pWasPressed = (activeButtons & (IConstants.ONE_INT << pButtonIndex))
                    != IConstants.ZERO_INT;
            if (pPressed && !pWasPressed) {
                dispatchButton(pButtonIndex, pState, true);
                activeButtons |= IConstants.ONE_INT << pButtonIndex;
            } else if (!pPressed && pWasPressed) {
                dispatchButton(pButtonIndex, pState, false);
                activeButtons &= ~(IConstants.ONE_INT << pButtonIndex);
            }
        }
        if (pAxesChanged && pPreviouslyActiveButtons != IConstants.ZERO_INT) {
            dispatchAxisUpdate(pState);
        }
        lastState = pState;
    }

    private void poll() {
        try {
            while (running.get()) {
                JoystickState pState = inputSource.readState();
                accept(pState);
                try {
                    Thread.sleep(IConstants.JOY_POLL_MS);
                } catch (InterruptedException pException) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        } catch (DeviceException pException) {
            running.set(false);
            releaseActiveButtons();
            try {
                failureListener.accept(pException);
            } catch (RuntimeException pListenerException) {
                pException.addSuppressed(pListenerException);
            }
        }
    }

    private boolean axesChanged(JoystickState pState) {
        if (lastState == null) {
            return true;
        }
        return pState.x() != lastState.x()
                || pState.y() != lastState.y()
                || pState.z() != lastState.z();
    }

    private void dispatchButton(
            int pButtonIndex,
            JoystickState pState,
            boolean pPressed) {
        try {
            dispatch(new SynthEvent(
                    IConstants.ZERO_LONG,
                    IConstants.SYNTH_ORGAN_ID,
                    pPressed ? IConstants.ACTION_ON : IConstants.ACTION_OFF,
                    IConstants.JOY_BASE_NOTE + pButtonIndex,
                    IConstants.ZERO_INT,
                    IConstants.JOY_VELOCITY,
                    pState.x(),
                    pState.y(),
                    pState.z()));
        } catch (SynthException pException) {
            throw new IllegalStateException(IConstants.ERR_JOY_READ, pException);
        }
    }

    private void dispatchAxisUpdate(JoystickState pState) {
        if (activeButtons == IConstants.ZERO_INT) {
            return;
        }
        for (int pButtonIndex = IConstants.ZERO_INT;
                pButtonIndex < IConstants.JOY_BUTTON_COUNT;
                pButtonIndex++) {
            if ((activeButtons & (IConstants.ONE_INT << pButtonIndex))
                    != IConstants.ZERO_INT) {
                dispatchButton(pButtonIndex, pState, true);
            }
        }
    }

    private void releaseActiveButtons() {
        if (activeButtons == IConstants.ZERO_INT) {
            return;
        }
        for (int pButtonIndex = IConstants.ZERO_INT;
                pButtonIndex < IConstants.JOY_BUTTON_COUNT;
                pButtonIndex++) {
            if ((activeButtons & (IConstants.ONE_INT << pButtonIndex)) != IConstants.ZERO_INT) {
                try {
                    dispatch(new SynthEvent(
                            IConstants.ZERO_LONG,
                            IConstants.SYNTH_ORGAN_ID,
                            IConstants.ACTION_OFF,
                            IConstants.JOY_BASE_NOTE + pButtonIndex,
                            IConstants.ZERO_INT,
                            IConstants.JOY_VELOCITY,
                            lastState == null ? IConstants.HUION_PRESSURE_MIN : lastState.x(),
                            lastState == null ? IConstants.HUION_PRESSURE_MIN : lastState.y(),
                            lastState == null ? IConstants.HUION_PRESSURE_MIN : lastState.z()));
                } catch (SynthException pException) {
                    throw new IllegalStateException(IConstants.ERR_JOY_READ, pException);
                }
            }
        }
        activeButtons = IConstants.ZERO_INT;
    }
}

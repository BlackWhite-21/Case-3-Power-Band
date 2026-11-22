package powerband.instrument;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import powerband.dto.SynthEvent;
import powerband.dto.TabletReading;
import powerband.exception.DeviceException;
import powerband.exception.SynthException;
import powerband.synth.SynthCommand;
import powerband.util.IConstants;

/**
 * Converts Huion readings to sustained saxophone events.
 */
public class HuionInstrument extends Instrument {

    private static final System.Logger LOGGER = System.getLogger(HuionInstrument.class.getName());

    private final HuionInputSource inputSource;
    private final HuionInputMapper mapper;
    private final Consumer<DeviceException> failureListener;

    public HuionInstrument(HuionInputSource pInputSource, SynthCommand pCommand,
            Consumer<SynthEvent> pEventListener) {
        this(pInputSource, pCommand, pEventListener,
                pException -> LOGGER.log(System.Logger.Level.ERROR, pException.getMessage(), pException));
    }

    public HuionInstrument(
            HuionInputSource pInputSource,
            SynthCommand pCommand,
            Consumer<SynthEvent> pEventListener,
            Consumer<DeviceException> pFailureListener) {
        super(pCommand, pEventListener);
        this.inputSource = Objects.requireNonNull(pInputSource);
        this.mapper = new HuionInputMapper();
        this.failureListener = Objects.requireNonNull(pFailureListener);
    }

    public void start() throws DeviceException {
        inputSource.start(this::accept, this::handleInputFailure);
    }

    public void stop() throws DeviceException {
        DeviceException stopFailure = null;
        try {
            inputSource.stop();
        } catch (DeviceException e) {
            stopFailure = e;
        }

        try {
            accept(new TabletReading(0f, 0f, 0f));
        } catch (IllegalStateException e) {
            if (stopFailure != null) {
                stopFailure.addSuppressed(e);
                throw stopFailure;
            }
            throw new DeviceException(IConstants.ERR_HUION_STOP, e);
        }
        if (stopFailure != null) {
            throw stopFailure;
        }
    }

    public void accept(TabletReading pReading) {
        List<SynthEvent> events = mapper.map(pReading);
        for (SynthEvent event : events) {
            try {
                dispatch(event);
            } catch (SynthException e) {
                throw new IllegalStateException(IConstants.ERR_HUION_COMMAND, e);
            }
        }
    }

    private void handleInputFailure(DeviceException pException) {
        try {
            accept(new TabletReading(
                    IConstants.HUION_COORDINATE_MIN,
                    IConstants.HUION_COORDINATE_MIN,
                    IConstants.HUION_PRESSURE_MIN));
        } catch (IllegalStateException pReleaseException) {
            pException.addSuppressed(pReleaseException);
        }
        try {
            failureListener.accept(pException);
        } catch (RuntimeException pListenerException) {
            pException.addSuppressed(pListenerException);
            LOGGER.log(System.Logger.Level.ERROR, pException.getMessage(), pException);
        }
    }
}

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

    private final HuionInputSource inputSource;
    private final HuionInputMapper mapper;

    public HuionInstrument(HuionInputSource pInputSource, SynthCommand pCommand,
            Consumer<SynthEvent> pEventListener) {
        super(pCommand, pEventListener);
        this.inputSource = Objects.requireNonNull(pInputSource);
        this.mapper = new HuionInputMapper();
    }

    public void start() throws DeviceException {
        inputSource.start(this::accept);
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
}

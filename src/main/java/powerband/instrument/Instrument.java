package powerband.instrument;

import java.util.Objects;
import java.util.function.Consumer;

import powerband.dto.SynthEvent;
import powerband.exception.SynthException;
import powerband.synth.SynthCommand;

/**
 * Bridge abstraction shared by hardware instruments.
 */
public abstract class Instrument {

    private final SynthCommand command;
    private final Consumer<SynthEvent> eventListener;

    protected Instrument(SynthCommand pCommand, Consumer<SynthEvent> pEventListener) {
        this.command = Objects.requireNonNull(pCommand);
        this.eventListener = Objects.requireNonNull(pEventListener);
    }

    protected final void dispatch(SynthEvent pEvent) throws SynthException {
        command.execute(pEvent);
        eventListener.accept(pEvent);
    }
}

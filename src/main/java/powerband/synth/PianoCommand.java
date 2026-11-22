package powerband.synth;

import static powerband.util.IConstants.PARAM_AMP;
import static powerband.util.IConstants.PARAM_DUR_MS;
import static powerband.util.IConstants.PARAM_NOTE;
import static powerband.util.IConstants.PARAM_THIRD;
import static powerband.util.IConstants.PIANO_AMP_MAX;
import static powerband.util.IConstants.PIANO_AMP_MIN;
import static powerband.util.IConstants.PIANO_DEFAULT_DUR_MS;
import static powerband.util.IConstants.PIANO_MAJOR_THIRD;
import static powerband.util.IConstants.PIANO_MAJOR_THRESHOLD;
import static powerband.util.IConstants.PIANO_MINOR_THIRD;
import static powerband.util.IConstants.SYNTH_PIANO_ID;
import static powerband.util.IConstants.SYNTH_PIANO_NAME;

import powerband.dto.SynthEvent;
import powerband.exception.SynthException;
import powerband.osc.OscClient;

/**
 * BRIDGE - ConcreteImplementor para \classicPiano.
 * timbre1 >= 0.5 → acorde mayor; si no, menor. velocity → volumen.
 */
public class PianoCommand extends SynthCommand {

    public PianoCommand(OscClient pClient) {
        super(pClient);
    }

    @Override
    public int getSynthId() {
        return SYNTH_PIANO_ID;
    }

    @Override
    public String getSynthName() {
        return SYNTH_PIANO_NAME;
    }

    @Override
    protected void play(SynthEvent pEvent) throws SynthException {
        int duration = pEvent.durMs() > 0 ? pEvent.durMs() : PIANO_DEFAULT_DUR_MS;
        int third = pEvent.timbre1() >= PIANO_MAJOR_THRESHOLD ? PIANO_MAJOR_THIRD : PIANO_MINOR_THIRD;
        getClient().newSynth(SYNTH_PIANO_NAME,
                PARAM_NOTE, pEvent.note(),
                PARAM_DUR_MS, duration,
                PARAM_THIRD, third,
                PARAM_AMP, scale(pEvent.velocity(), PIANO_AMP_MIN, PIANO_AMP_MAX));
    }
}

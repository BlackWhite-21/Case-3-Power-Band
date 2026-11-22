package powerband.synth;

import static powerband.util.IConstants.BASS_AMP_MAX;
import static powerband.util.IConstants.BASS_AMP_MIN;
import static powerband.util.IConstants.BASS_DEFAULT_DUR_MS;
import static powerband.util.IConstants.PARAM_AMP;
import static powerband.util.IConstants.PARAM_DUR_MS;
import static powerband.util.IConstants.PARAM_NOTE;
import static powerband.util.IConstants.SYNTH_BASS_ID;
import static powerband.util.IConstants.SYNTH_BASS_NAME;

import powerband.dto.SynthEvent;
import powerband.exception.SynthException;
import powerband.osc.OscClient;

/**
 * BRIDGE - ConcreteImplementor para \stringBass. velocity → volumen.
 */
public class BassCommand extends SynthCommand {

    public BassCommand(OscClient pClient) {
        super(pClient);
    }

    @Override
    public int getSynthId() {
        return SYNTH_BASS_ID;
    }

    @Override
    public String getSynthName() {
        return SYNTH_BASS_NAME;
    }

    @Override
    protected void play(SynthEvent pEvent) throws SynthException {
        int duration = pEvent.durMs() > 0 ? pEvent.durMs() : BASS_DEFAULT_DUR_MS;
        getClient().newSynth(SYNTH_BASS_NAME,
                PARAM_NOTE, pEvent.note(),
                PARAM_DUR_MS, duration,
                PARAM_AMP, scale(pEvent.velocity(), BASS_AMP_MIN, BASS_AMP_MAX));
    }
}

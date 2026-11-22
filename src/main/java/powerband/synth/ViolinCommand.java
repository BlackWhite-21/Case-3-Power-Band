package powerband.synth;

import static powerband.util.IConstants.SYNTH_VIOLIN_ID;
import static powerband.util.IConstants.SYNTH_VIOLIN_NAME;

import powerband.osc.OscClient;

/**
 * Bridge implementation for the sustained violin synth.
 */
public class ViolinCommand extends GateSynthCommand {

    public ViolinCommand(OscClient pClient) {
        super(pClient);
    }

    @Override
    public int getSynthId() {
        return SYNTH_VIOLIN_ID;
    }

    @Override
    public String getSynthName() {
        return SYNTH_VIOLIN_NAME;
    }
}
package powerband.synth;

import static powerband.util.IConstants.SYNTH_SAXOPHONE_ID;
import static powerband.util.IConstants.SYNTH_SAXOPHONE_NAME;

import powerband.osc.OscClient;

/**
 * Bridge implementation for the sustained saxophone synth.
 */
public class SaxophoneCommand extends GateSynthCommand {

    public SaxophoneCommand(OscClient pClient) {
        super(pClient);
    }

    @Override
    public int getSynthId() {
        return SYNTH_SAXOPHONE_ID;
    }

    @Override
    public String getSynthName() {
        return SYNTH_SAXOPHONE_NAME;
    }
}
package powerband.synth;

import static powerband.util.IConstants.ORGAN_A1;
import static powerband.util.IConstants.ORGAN_A2_MAX;
import static powerband.util.IConstants.ORGAN_A2_MIN;
import static powerband.util.IConstants.ORGAN_A3_FACTOR;
import static powerband.util.IConstants.ORGAN_AMP_MAX;
import static powerband.util.IConstants.ORGAN_AMP_MIN;
import static powerband.util.IConstants.ORGAN_DEFAULT_DUR_MS;
import static powerband.util.IConstants.ORGAN_R1;
import static powerband.util.IConstants.ORGAN_R2_MAX;
import static powerband.util.IConstants.ORGAN_R2_MIN;
import static powerband.util.IConstants.ORGAN_R3_MAX;
import static powerband.util.IConstants.ORGAN_R3_MIN;
import static powerband.util.IConstants.OSC_GATE;
import static powerband.util.IConstants.OSC_GATE_OFF;
import static powerband.util.IConstants.PARAM_A1;
import static powerband.util.IConstants.PARAM_A2;
import static powerband.util.IConstants.PARAM_A3;
import static powerband.util.IConstants.PARAM_AMP;
import static powerband.util.IConstants.PARAM_FREQ;
import static powerband.util.IConstants.PARAM_R1;
import static powerband.util.IConstants.PARAM_R2;
import static powerband.util.IConstants.PARAM_R3;
import static powerband.util.IConstants.SYNTH_ORGAN_ID;
import static powerband.util.IConstants.SYNTH_ORGAN_NAME;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import powerband.dto.SynthEvent;
import powerband.exception.SynthException;
import powerband.osc.OscClient;

/**
 * BRIDGE - ConcreteImplementor para \organ70s, que suena hasta recibir gate = 0.
 * timbre1 → parcial 2, timbre2 → parcial 3, timbre3 → amplitud de los parciales.
 */
public class OrganCommand extends SynthCommand {

    private final Map<Integer, Integer> activeNodes;      // nota MIDI → id del nodo en scsynth
    private final ScheduledExecutorService releaser;

    public OrganCommand(OscClient pClient) {
        super(pClient);
        this.activeNodes = new ConcurrentHashMap<>();
        this.releaser = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable);
            thread.setDaemon(true);
            return thread;
        });
    }

    @Override
    public int getSynthId() {
        return SYNTH_ORGAN_ID;
    }

    @Override
    public String getSynthName() {
        return SYNTH_ORGAN_NAME;
    }

    @Override
    protected void start(SynthEvent pEvent) throws SynthException {
        releaseNote(pEvent.note());   // re-disparo seguro, igual que myband.sc
        float amp2 = scale(pEvent.timbre3(), ORGAN_A2_MIN, ORGAN_A2_MAX);
        int nodeId = getClient().newSynth(SYNTH_ORGAN_NAME,
                PARAM_FREQ, midiToFreq(pEvent.note()),
                PARAM_AMP, scale(pEvent.velocity(), ORGAN_AMP_MIN, ORGAN_AMP_MAX),
                PARAM_R1, ORGAN_R1,
                PARAM_R2, scale(pEvent.timbre1(), ORGAN_R2_MIN, ORGAN_R2_MAX),
                PARAM_R3, scale(pEvent.timbre2(), ORGAN_R3_MIN, ORGAN_R3_MAX),
                PARAM_A1, ORGAN_A1,
                PARAM_A2, amp2,
                PARAM_A3, amp2 * ORGAN_A3_FACTOR);
        activeNodes.put(pEvent.note(), nodeId);
    }

    @Override
    protected void stop(SynthEvent pEvent) throws SynthException {
        releaseNote(pEvent.note());
    }

    /** Si otro instrumento (vía Bridge) manda una nota con duración, se enciende y se programa su apagado. */
    @Override
    protected void play(SynthEvent pEvent) throws SynthException {
        start(pEvent);
        int duration = pEvent.durMs() > 0 ? pEvent.durMs() : ORGAN_DEFAULT_DUR_MS;
        int note = pEvent.note();
        releaser.schedule(() -> {
            try {
                releaseNote(note);
            } catch (SynthException e) {
                System.err.println(e.getMessage());
            }
        }, duration, TimeUnit.MILLISECONDS);
    }

    @Override
    public void stopAll() throws SynthException {
        for (Integer note : activeNodes.keySet()) {
            releaseNote(note);
        }
    }

    private void releaseNote(int pNote) throws SynthException {
        Integer nodeId = activeNodes.remove(pNote);
        if (nodeId != null) {
            getClient().setControl(nodeId, OSC_GATE, OSC_GATE_OFF);
        }
    }
}

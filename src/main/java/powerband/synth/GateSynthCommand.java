package powerband.synth;

import static powerband.util.IConstants.DEFAULT_SUSTAINED_DUR_MS;
import static powerband.util.IConstants.OSC_GATE_OFF;
import static powerband.util.IConstants.PARAM_AMP;
import static powerband.util.IConstants.PARAM_GATE;
import static powerband.util.IConstants.PARAM_NOTE;
import static powerband.util.IConstants.SUSTAINED_AMP_MAX;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import powerband.dto.SynthEvent;
import powerband.exception.SynthException;
import powerband.osc.OscClient;

/**
 * Shared implementation for gate-controlled melodic synths.
 */
abstract class GateSynthCommand extends SynthCommand {

    private final Map<Integer, Integer> activeNodes = new ConcurrentHashMap<>();
    private final ScheduledExecutorService releaser = Executors.newSingleThreadScheduledExecutor(pRunnable -> {
        Thread thread = new Thread(pRunnable, "powerband-synth-releaser");
        thread.setDaemon(true);
        return thread;
    });

    protected GateSynthCommand(OscClient pClient) {
        super(pClient);
    }

    @Override
    protected synchronized void start(SynthEvent pEvent) throws SynthException {
        int note = pEvent.note();
        float amp = scale(pEvent.velocity(), 0f, SUSTAINED_AMP_MAX);
        Integer nodeId = activeNodes.get(note);
        if (nodeId == null) {
            nodeId = getClient().newSynth(getSynthName(),
                    PARAM_NOTE, note,
                    PARAM_AMP, amp);
            activeNodes.put(note, nodeId);
        } else {
            getClient().setControl(nodeId, PARAM_AMP, amp);
        }
    }

    @Override
    protected synchronized void stop(SynthEvent pEvent) throws SynthException {
        releaseNote(pEvent.note(), null);
    }

    @Override
    protected void play(SynthEvent pEvent) throws SynthException {
        start(pEvent);
        int duration = pEvent.durMs() > 0 ? pEvent.durMs() : DEFAULT_SUSTAINED_DUR_MS;
        Integer nodeId = activeNodes.get(pEvent.note());
        if (nodeId != null) {
            releaser.schedule(() -> {
                try {
                    releaseNote(pEvent.note(), nodeId);
                } catch (SynthException e) {
                    System.err.println(e.getMessage());
                }
            }, duration, TimeUnit.MILLISECONDS);
        }
    }

    @Override
    public synchronized void stopAll() throws SynthException {
        for (Integer note : activeNodes.keySet().toArray(Integer[]::new)) {
            releaseNote(note, null);
        }
    }

    private synchronized void releaseNote(int pNote, Integer pExpectedNodeId) throws SynthException {
        Integer nodeId = activeNodes.get(pNote);
        if (nodeId != null && (pExpectedNodeId == null || nodeId.equals(pExpectedNodeId))) {
            activeNodes.remove(pNote);
            getClient().setControl(nodeId, PARAM_GATE, OSC_GATE_OFF);
        }
    }
}

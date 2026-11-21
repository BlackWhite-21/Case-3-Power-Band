package powerband.synth;

import static powerband.util.IConstants.ACTION_OFF;
import static powerband.util.IConstants.ACTION_ON;
import static powerband.util.IConstants.MIDI_A4_FREQ;
import static powerband.util.IConstants.MIDI_A4_NOTE;
import static powerband.util.IConstants.SEMITONES_PER_OCTAVE;

import powerband.dto.SynthEvent;
import powerband.exception.SynthException;
import powerband.osc.OscClient;

/**
 * BRIDGE - Implementor. Jerarquía de comandos que invocan a los synths de SuperCollider.
 * Cada subclase sabe traducir un {@link SynthEvent} genérico a los parámetros de SU synth.
 * Los instrumentos (la abstracción) no saben nada de OSC ni de los parámetros del synth.
 */
public abstract class SynthCommand {

    private final OscClient client;

    protected SynthCommand(OscClient pClient) {
        this.client = pClient;
    }

    public abstract int getSynthId();

    public abstract String getSynthName();

    /** Nota con duración: el synth se apaga solo. */
    protected abstract void play(SynthEvent pEvent) throws SynthException;

    /** Nota sostenida. Por defecto un synth de duración fija la toca como play. */
    protected void start(SynthEvent pEvent) throws SynthException {
        play(pEvent);
    }

    /** Fin de nota sostenida. Por defecto no hace nada (piano y bajo se apagan solos). */
    protected void stop(SynthEvent pEvent) throws SynthException {
    }

    /** Apaga todo lo que siga sonando. */
    public void stopAll() throws SynthException {
    }

    /** Método plantilla: decide según la acción del evento. */
    public final void execute(SynthEvent pEvent) throws SynthException {
        switch (pEvent.action()) {
            case ACTION_ON -> start(pEvent);
            case ACTION_OFF -> stop(pEvent);
            default -> play(pEvent);
        }
    }

    protected OscClient getClient() {
        return client;
    }

    /** Escala lineal 0..1 → [min, max]. */
    protected float scale(float pValue, float pMin, float pMax) {
        float clamped = Math.max(0f, Math.min(1f, pValue));
        return pMin + clamped * (pMax - pMin);
    }

    protected float midiToFreq(int pNote) {
        return (float) (MIDI_A4_FREQ * Math.pow(2.0, (pNote - MIDI_A4_NOTE) / SEMITONES_PER_OCTAVE));
    }
}

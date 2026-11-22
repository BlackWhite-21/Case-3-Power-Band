package powerband.dto;

/**
 * Un comando enviado a un synth. Es lo que se monitorea, se graba en disco y se reproduce.
 * Inmutable: para cambiar el tiempo se crea una copia con {@link #withOffset(long)}.
 *
 * @param offsetMs ms desde el inicio de la grabación (0 si no se está grabando)
 * @param synthId  ID of one of the registered SuperCollider synth commands
 * @param action   ACTION_PLAY, ACTION_ON o ACTION_OFF
 * @param note     nota MIDI
 * @param durMs    duración en ms (solo ACTION_PLAY)
 * @param velocity intensidad 0..1
 * @param timbre1  control libre 0..1 que el dispositivo entrega
 * @param timbre2  control libre 0..1
 * @param timbre3  control libre 0..1
 */
public record SynthEvent(long offsetMs, int synthId, int action, int note, int durMs,
        float velocity, float timbre1, float timbre2, float timbre3) {

    public SynthEvent withOffset(long pOffsetMs) {
        return new SynthEvent(pOffsetMs, synthId, action, note, durMs, velocity, timbre1, timbre2, timbre3);
    }
}

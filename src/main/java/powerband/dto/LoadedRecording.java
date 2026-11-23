package powerband.dto;

/**
 * Header and byte bounds of a recording selected for playback.
 */
public record LoadedRecording(
        String name,
        long headerPosition,
        long endOffset,
        long recordedAtEpochMillis) {
}

package powerband.dto;

public record LoadedRecording(
        String name,
        long headerPosition,
        long endOffset,
        long recordedAtEpochMillis) {
}

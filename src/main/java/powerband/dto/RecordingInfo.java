package powerband.dto;

import static powerband.util.IConstants.LIST_DISPLAY_FORMAT;

/**
 * Una línea de recordings.list: nombre y fecha/hora en que se grabó.
 */
public record RecordingInfo(String name, String recordedAt) {

    @Override
    public String toString() {
        return String.format(LIST_DISPLAY_FORMAT, recordedAt, name);
    }
}

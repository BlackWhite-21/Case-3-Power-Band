package powerband.instrument;

import powerband.exception.DeviceException;

/**
 * Injectable PCM source for the default system microphone and its test doubles.
 */
public interface MicrophoneAudioSource {

    void open() throws DeviceException;

    float[] readFrame() throws DeviceException;

    void close() throws DeviceException;
}

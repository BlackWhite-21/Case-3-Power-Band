package powerband.instrument;

import java.util.function.Consumer;

import powerband.dto.TabletReading;
import powerband.exception.DeviceException;

/**
 * Hardware boundary for a Huion-compatible driver that exposes pen pressure.
 */
public interface HuionInputSource {

    void start(Consumer<TabletReading> pReadingConsumer) throws DeviceException;

    void stop() throws DeviceException;
}

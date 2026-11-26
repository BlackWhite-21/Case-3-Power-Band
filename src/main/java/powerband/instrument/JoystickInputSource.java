package powerband.instrument;

import powerband.dto.JoystickState;
import powerband.exception.DeviceException;

public interface JoystickInputSource {

    void open() throws DeviceException;

    JoystickState readState() throws DeviceException;

    void close();
}

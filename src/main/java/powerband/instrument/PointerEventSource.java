package powerband.instrument;

import java.awt.Component;

import powerband.dto.DatosTableta;
import powerband.exception.DeviceException;

interface PointerEventSource {

    void attach(Component pComponent) throws DeviceException;

    DatosTableta pollEvent(int pTimeoutMs) throws DeviceException;

    void requestStop();

    void detach() throws DeviceException;
}

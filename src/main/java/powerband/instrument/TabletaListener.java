package powerband.instrument;

import powerband.dto.DatosTableta;
import powerband.exception.DeviceException;

@FunctionalInterface
public interface TabletaListener {

    void onDatosTableta(DatosTableta pDatos);

    default void onError(DeviceException pException) {
    }
}

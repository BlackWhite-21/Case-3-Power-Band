package powerband.dto;

import java.util.Objects;

import powerband.util.IConstants;

public record DatosTableta(double x, double y, float pressure, EstadoPluma estado) {

    public DatosTableta {
        if (!Double.isFinite(x) || !Double.isFinite(y)) {
            throw new IllegalArgumentException(IConstants.ERR_HUION_POINTER_COORDINATES);
        }
        if (!Float.isFinite(pressure) || pressure < IConstants.HUION_PRESSURE_MIN
                || pressure > IConstants.HUION_PRESSURE_MAX) {
            throw new IllegalArgumentException(IConstants.ERR_HUION_POINTER_EVENT);
        }
        Objects.requireNonNull(estado);
    }
}

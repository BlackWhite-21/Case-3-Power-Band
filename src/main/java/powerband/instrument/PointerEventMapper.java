package powerband.instrument;

import powerband.dto.DatosTableta;
import powerband.dto.EstadoPluma;
import powerband.exception.DeviceException;
import powerband.util.IConstants;

final class PointerEventMapper {

    private PointerEventMapper() {
    }

    static DatosTableta map(NativePointerEvent pEvent) throws DeviceException {
        if (!pEvent.coordinatesAvailable()) {
            throw new DeviceException(IConstants.ERR_HUION_POINTER_COORDINATES);
        }
        if (pEvent.scaleX() <= IConstants.HUION_POINTER_INVALID_SCALE
                || pEvent.scaleY() <= IConstants.HUION_POINTER_INVALID_SCALE
                || !Double.isFinite(pEvent.scaleX())
                || !Double.isFinite(pEvent.scaleY())) {
            throw new DeviceException(IConstants.ERR_HUION_POINTER_COORDINATES);
        }

        EstadoPluma estado = getState(pEvent.message());
        if (estado == null) {
            return null;
        }

        double x = pEvent.x() / pEvent.scaleX() - pEvent.componentX();
        double y = pEvent.y() / pEvent.scaleY() - pEvent.componentY();
        if (x < IConstants.HUION_COORDINATE_MIN
                || y < IConstants.HUION_COORDINATE_MIN
                || x >= pEvent.componentWidth()
                || y >= pEvent.componentHeight()) {
            estado = EstadoPluma.FUERA_SUPERFICIE;
        }

        float pressure = IConstants.HUION_PRESSURE_MIN;
        if (estado == EstadoPluma.PRESIONADO || estado == EstadoPluma.MOVIMIENTO) {
            if (!pEvent.pressureAvailable()) {
                throw new DeviceException(IConstants.ERR_HUION_POINTER_UNSUPPORTED);
            }
            if (pEvent.pressure() < IConstants.HUION_POINTER_RAW_PRESSURE_MIN
                    || pEvent.pressure() > IConstants.HUION_POINTER_PRESSURE_MAX) {
                throw new DeviceException(IConstants.ERR_HUION_POINTER_EVENT);
            }
            if (estado == EstadoPluma.PRESIONADO || pEvent.inContact()) {
                pressure = (float) pEvent.pressure() / IConstants.HUION_POINTER_PRESSURE_MAX;
            }
        }

        if (estado == EstadoPluma.FUERA_SUPERFICIE || estado == EstadoPluma.LIBERADO) {
            pressure = IConstants.HUION_PRESSURE_MIN;
        }
        return new DatosTableta(x, y, pressure, estado);
    }

    private static EstadoPluma getState(int pMessage) {
        return switch (pMessage) {
            case IConstants.HUION_WM_POINTER_DOWN -> EstadoPluma.PRESIONADO;
            case IConstants.HUION_WM_POINTER_UPDATE -> EstadoPluma.MOVIMIENTO;
            case IConstants.HUION_WM_POINTER_UP,
                    IConstants.HUION_WM_POINTER_CAPTURE_CHANGED -> EstadoPluma.LIBERADO;
            case IConstants.HUION_WM_POINTER_LEAVE -> EstadoPluma.FUERA_SUPERFICIE;
            default -> null;
        };
    }
}

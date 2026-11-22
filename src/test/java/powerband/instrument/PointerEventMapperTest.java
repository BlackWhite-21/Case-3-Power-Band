package powerband.instrument;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static powerband.util.IConstants.HUION_WM_POINTER_DOWN;
import static powerband.util.IConstants.HUION_WM_POINTER_LEAVE;
import static powerband.util.IConstants.HUION_WM_POINTER_UPDATE;

import org.junit.jupiter.api.Test;

import powerband.dto.DatosTableta;
import powerband.dto.EstadoPluma;
import powerband.exception.DeviceException;

class PointerEventMapperTest {

    @Test
    void normalizesRealPressureAndConvertsHiDpiComponentCoordinates() throws Exception {
        NativePointerEvent pEvent = new NativePointerEvent(
                HUION_WM_POINTER_DOWN, 512, 240, 120, true, true, true,
                2.0, 2.0, 20, 10, 200, 100);

        DatosTableta pData = PointerEventMapper.map(pEvent);

        assertEquals(100.0, pData.x());
        assertEquals(50.0, pData.y());
        assertEquals(0.5f, pData.pressure());
        assertEquals(EstadoPluma.PRESIONADO, pData.estado());
    }

    @Test
    void hoverMovementHasZeroPressureAndReleaseAlwaysClearsPressure() throws Exception {
        NativePointerEvent pHover = new NativePointerEvent(
                HUION_WM_POINTER_UPDATE, 0, 40, 60, true, false, true,
                1.0, 1.0, 0, 0, 100, 100);
        NativePointerEvent pLeave = new NativePointerEvent(
                HUION_WM_POINTER_LEAVE, 800, 40, 60, true, false, true,
                1.0, 1.0, 0, 0, 100, 100);

        DatosTableta pMovement = PointerEventMapper.map(pHover);
        DatosTableta pOutside = PointerEventMapper.map(pLeave);

        assertEquals(0f, pMovement.pressure());
        assertEquals(EstadoPluma.MOVIMIENTO, pMovement.estado());
        assertEquals(0f, pOutside.pressure());
        assertEquals(EstadoPluma.FUERA_SUPERFICIE, pOutside.estado());
    }

    @Test
    void reportsUnsupportedPressureInsteadOfInventingAReading() {
        NativePointerEvent pEvent = new NativePointerEvent(
                HUION_WM_POINTER_UPDATE, 0, 10, 10, false, true, true,
                1.0, 1.0, 0, 0, 100, 100);

        assertThrows(DeviceException.class,
                () -> PointerEventMapper.map(pEvent));
    }

    @Test
    void reportsCoordinatesOutsideTheRegisteredComponentAsOutOfSurface() throws Exception {
        NativePointerEvent pEvent = new NativePointerEvent(
                HUION_WM_POINTER_UPDATE, 700, 110, 30, true, true, true,
                1.0, 1.0, 0, 0, 100, 100);

        DatosTableta pData = PointerEventMapper.map(pEvent);

        assertEquals(EstadoPluma.FUERA_SUPERFICIE, pData.estado());
        assertEquals(0f, pData.pressure());
    }

    @Test
    void rejectsInvalidPressureAndCoordinateTransforms() {
        NativePointerEvent pInvalidPressure = new NativePointerEvent(
                HUION_WM_POINTER_DOWN, 1025, 10, 10, true, true, true,
                1.0, 1.0, 0, 0, 100, 100);
        NativePointerEvent pValid = new NativePointerEvent(
                HUION_WM_POINTER_DOWN, 100, 10, 10, true, true, true,
                0.0, 1.0, 0, 0, 100, 100);

        assertThrows(DeviceException.class,
                () -> PointerEventMapper.map(pInvalidPressure));
        assertThrows(DeviceException.class, () -> PointerEventMapper.map(pValid));
    }
}

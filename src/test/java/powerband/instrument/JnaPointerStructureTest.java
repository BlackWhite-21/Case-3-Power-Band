package powerband.instrument;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;

class JnaPointerStructureTest {

    @Test
    void nativeStructuresHaveConsistentFieldLayouts() throws Exception {
        Structure pWinPoint = createStructure("WinPoint");
        Structure pWinPointReference = createStructure("WinPointReference", 0, 0);
        Structure pPointerInfo = createStructure("PointerInfo");
        Structure pPointerPenInfo = createStructure("PointerPenInfo");
        Structure pNativeMessage = createStructure("NativeMessage", Pointer.NULL);

        assertEquals(Integer.BYTES * 2, pWinPoint.size());
        assertEquals(Integer.BYTES * 2, pWinPointReference.size());
        assertTrue(Structure.ByReference.class.isAssignableFrom(pWinPointReference.getClass()));
        assertFalse(Structure.ByValue.class.isAssignableFrom(pWinPointReference.getClass()));
        assertTrue(pPointerInfo.size() > pWinPoint.size());
        assertEquals(pPointerInfo.size() + Integer.BYTES * 6, pPointerPenInfo.size());
        assertTrue(pNativeMessage.size() > pWinPoint.size());

        if (Native.POINTER_SIZE == Long.BYTES) {
            assertEquals(48, pNativeMessage.size());
            assertEquals(96, pPointerInfo.size());
            assertEquals(120, pPointerPenInfo.size());
            assertEquals(8, pWinPoint.size());
            assertEquals(8, fieldOffset(pNativeMessage, "message"));
            assertEquals(36, fieldOffset(pNativeMessage, "point"));
            assertEquals(16, fieldOffset(pPointerInfo, "sourceDevice"));
            assertEquals(32, fieldOffset(pPointerInfo, "pixelLocation"));
            assertEquals(80, fieldOffset(pPointerInfo, "performanceCount"));
            assertEquals(96, fieldOffset(pPointerPenInfo, "penFlags"));
        }
    }

    private int fieldOffset(Structure pStructure, String pFieldName) throws Exception {
        Method pFieldOffset = Structure.class.getDeclaredMethod("fieldOffset", String.class);
        pFieldOffset.setAccessible(true);
        return (int) pFieldOffset.invoke(pStructure, pFieldName);
    }

    private Structure createStructure(String pSimpleName) throws Exception {
        Class<?> pStructureType = Class.forName(
                JnaPointerInputSource.class.getName() + "$" + pSimpleName);
        Constructor<?> pConstructor = pStructureType.getDeclaredConstructor();
        pConstructor.setAccessible(true);
        return (Structure) pConstructor.newInstance();
    }

    private Structure createStructure(String pSimpleName, Pointer pPointer) throws Exception {
        Class<?> pStructureType = Class.forName(
                JnaPointerInputSource.class.getName() + "$" + pSimpleName);
        Constructor<?> pConstructor = pStructureType.getDeclaredConstructor(Pointer.class);
        pConstructor.setAccessible(true);
        return (Structure) pConstructor.newInstance(new Object[] { pPointer });
    }

    private Structure createStructure(String pSimpleName, int pX, int pY) throws Exception {
        Class<?> pStructureType = Class.forName(
                JnaPointerInputSource.class.getName() + "$" + pSimpleName);
        Constructor<?> pConstructor = pStructureType.getDeclaredConstructor(int.class, int.class);
        pConstructor.setAccessible(true);
        return (Structure) pConstructor.newInstance(pX, pY);
    }
}

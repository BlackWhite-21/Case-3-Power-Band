package powerband.instrument;

import java.util.Locale;
import java.util.Map;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Structure;
import com.sun.jna.FunctionMapper;
import com.sun.jna.win32.StdCallLibrary;

import powerband.dto.JoystickState;
import powerband.exception.DeviceException;
import powerband.util.IConstants;

public final class WinmmJoystickInputSource implements JoystickInputSource {

    private volatile WinmmApi api;
    private volatile boolean opened;
    private volatile int joystickId;

    @Override
    public void open() throws DeviceException {
        ensureSupportedPlatform();
        try {
            WinmmApi pApi = Native.load(
                    IConstants.JOY_LIBRARY,
                    WinmmApi.class,
                    Map.of(Library.OPTION_FUNCTION_MAPPER,
                            (FunctionMapper) (pLibrary, pMethod) -> pMethod.getName()));
            for (int pJoystickId = IConstants.ZERO_INT;
                    pJoystickId < IConstants.JOY_MAX_DEVICES;
                    pJoystickId++) {
                JoyInfoEx pInfo = new JoyInfoEx();
                if (pApi.joyGetPosEx(pJoystickId, pInfo) == IConstants.JOY_NO_ERROR) {
                    api = pApi;
                    joystickId = pJoystickId;
                    opened = true;
                    return;
                }
            }
            throw new DeviceException(IConstants.ERR_NO_JOYSTICK);
        } catch (UnsatisfiedLinkError | IllegalArgumentException | IllegalStateException pException) {
            throw new DeviceException(IConstants.ERR_JOY_READ, pException);
        }
    }

    @Override
    public JoystickState readState() throws DeviceException {
        WinmmApi pApi = api;
        if (!opened || pApi == null) {
            throw new DeviceException(IConstants.ERR_JOY_READ);
        }
        try {
            JoyInfoEx pInfo = new JoyInfoEx();
            if (pApi.joyGetPosEx(joystickId, pInfo) != IConstants.JOY_NO_ERROR) {
                throw new DeviceException(IConstants.ERR_JOY_READ);
            }
            float pX = normalizeAxis(pInfo.dwXpos);
            float pY = normalizeAxis(pInfo.dwYpos);
            float pZ = normalizeAxis(pInfo.dwZpos);
            return new JoystickState(pX, pY, pZ, pInfo.dwButtons);
        } catch (RuntimeException pException) {
            throw new DeviceException(IConstants.ERR_JOY_READ, pException);
        }
    }

    @Override
    public void close() {
        opened = false;
        api = null;
    }

    private static float normalizeAxis(int pValue) {
        float pNormalized = pValue / IConstants.JOY_AXIS_MAX;
        return Math.clamp(pNormalized, IConstants.HUION_PRESSURE_MIN, IConstants.HUION_COORDINATE_MAX);
    }

    private static void ensureSupportedPlatform() throws DeviceException {
        String pOsName = System.getProperty(IConstants.OS_NAME_PROPERTY);
        String pArchitecture = System.getProperty(IConstants.OS_ARCH_PROPERTY);
        if (pOsName == null
                || !pOsName.toLowerCase(Locale.ROOT).startsWith(IConstants.OS_WINDOWS_PREFIX)
                || !IConstants.HUION_POINTER_ARCH_AMD64.equalsIgnoreCase(pArchitecture)) {
            throw new DeviceException(IConstants.ERR_JOY_NOT_WINDOWS);
        }
    }

    interface WinmmApi extends StdCallLibrary {

        int joyGetPosEx(int pJoystickId, JoyInfoEx pInfo);
    }

    @Structure.FieldOrder({
        "dwSize",
        "dwFlags",
        "dwXpos",
        "dwYpos",
        "dwZpos",
        "dwRpos",
        "dwUpos",
        "dwVpos",
        "dwButtons",
        "dwButtonNumber",
        "dwPOV",
        "dwReserved1",
        "dwReserved2"
    })
    public static final class JoyInfoEx extends Structure {

        public int dwSize = IConstants.JOYINFOEX_SIZE;
        public int dwFlags = IConstants.JOY_RETURN_ALL;
        public int dwXpos;
        public int dwYpos;
        public int dwZpos;
        public int dwRpos;
        public int dwUpos;
        public int dwVpos;
        public int dwButtons;
        public int dwButtonNumber;
        public int dwPOV;
        public int dwReserved1;
        public int dwReserved2;

        public JoyInfoEx() {
            super();
        }
    }
}

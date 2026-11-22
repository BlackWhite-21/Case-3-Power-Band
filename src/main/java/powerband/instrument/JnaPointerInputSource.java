package powerband.instrument;

import java.awt.Component;
import java.awt.EventQueue;
import java.awt.GraphicsConfiguration;
import java.awt.Point;
import java.awt.Window;
import java.awt.geom.AffineTransform;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.SwingUtilities;

import com.sun.jna.FunctionMapper;
import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.win32.StdCallLibrary;

import powerband.dto.DatosTableta;
import powerband.exception.DeviceException;
import powerband.util.IConstants;

final class JnaPointerInputSource implements PointerEventSource {

    private final ArrayBlockingQueue<NativePointerEvent> events =
            new ArrayBlockingQueue<>(IConstants.HUION_POINTER_QUEUE_CAPACITY);
    private final AtomicReference<DeviceException> failure = new AtomicReference<>();
    private final Object callbackLock = new Object();
    private volatile WindowsPointerApi windowsPointerApi;
    private volatile Component component;
    private volatile Window window;
    private volatile Pointer windowHandle;
    private volatile Pointer hookHandle;
    private volatile boolean stopping;
    private boolean detaching;
    private int activeCallbacks;
    private volatile int lastPenPointerId = IConstants.HUION_NO_ACTIVE_POINTER;
    private volatile int lastClientX;
    private volatile int lastClientY;

    @Override
    public void attach(Component pComponent) throws DeviceException {
        if (!EventQueue.isDispatchThread()) {
            throw new DeviceException(IConstants.ERR_HUION_POINTER_INITIALIZE);
        }
        ensureSupportedPlatform();
        WindowsPointerApi pWindowsPointerApi = getWindowsPointerApi();
        Window pWindow = SwingUtilities.getWindowAncestor(pComponent);
        if (pWindow == null || !pWindow.isDisplayable()) {
            throw new DeviceException(IConstants.ERR_HUION_POINTER_NOT_DISPLAYABLE);
        }

        try {
            Pointer pWindowHandle = new Pointer(Native.getComponentID(pWindow));
            if (Pointer.nativeValue(pWindowHandle) == IConstants.NATIVE_NULL_POINTER) {
                throw new DeviceException(IConstants.ERR_HUION_POINTER_INITIALIZE);
            }
            int pWindowThreadId = pWindowsPointerApi.GetWindowThreadProcessId(
                    pWindowHandle,
                    new IntByReference());
            if (pWindowThreadId == IConstants.NATIVE_ZERO) {
                throw new DeviceException(IConstants.ERR_HUION_POINTER_INITIALIZE);
            }

            PointerHook pHookProcedure = this::onGetMessage;
            Pointer pHookHandle = pWindowsPointerApi.SetWindowsHookExW(
                    IConstants.HUION_POINTER_HOOK_GET_MESSAGE,
                    pHookProcedure,
                    null,
                    pWindowThreadId);
            if (pHookHandle == null
                    || Pointer.nativeValue(pHookHandle) == IConstants.NATIVE_NULL_POINTER) {
                throw new DeviceException(IConstants.ERR_HUION_POINTER_INITIALIZE);
            }
            this.component = pComponent;
            this.window = pWindow;
            this.windowHandle = pWindowHandle;
            this.windowsPointerApi = pWindowsPointerApi;
            this.hookHandle = pHookHandle;
        } catch (UnsatisfiedLinkError | IllegalArgumentException e) {
            throw new DeviceException(IConstants.ERR_HUION_POINTER_API, e);
        }
    }

    @Override
    public DatosTableta pollEvent(int pTimeoutMs) throws DeviceException {
        try {
            NativePointerEvent pEvent = events.poll(pTimeoutMs, TimeUnit.MILLISECONDS);
            if (pEvent == null) {
                DeviceException pFailure = failure.get();
                if (pFailure != null) {
                    throw pFailure;
                }
                return null;
            }
            return PointerEventMapper.map(pEvent);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new DeviceException(IConstants.ERR_HUION_POINTER_EVENT, e);
        }
    }

    @Override
    public void requestStop() {
        stopping = true;
    }

    @Override
    public void detach() throws DeviceException {
        stopping = true;
        synchronized (callbackLock) {
            detaching = true;
        }
        Pointer pHookHandle = hookHandle;
        WindowsPointerApi pWindowsPointerApi = windowsPointerApi;
        if (pHookHandle != null && !pWindowsPointerApi.UnhookWindowsHookEx(pHookHandle)) {
            throw new DeviceException(IConstants.ERR_HUION_POINTER_CLOSE);
        }
        synchronized (callbackLock) {
            while (activeCallbacks > IConstants.NATIVE_ZERO) {
                try {
                    callbackLock.wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new DeviceException(IConstants.ERR_HUION_POINTER_CLOSE, e);
                }
            }
            hookHandle = null;
            windowHandle = null;
            windowsPointerApi = null;
            window = null;
            component = null;
        }
        events.clear();
    }

    private Pointer onGetMessage(int pCode, Pointer pWParam, Pointer pLParam) {
        WindowsPointerApi pWindowsPointerApi;
        Pointer pHookHandle;
        boolean pCaptureMessage;
        synchronized (callbackLock) {
            pWindowsPointerApi = windowsPointerApi == null
                    ? WindowsPointerApiHolder.INSTANCE
                    : windowsPointerApi;
            pHookHandle = hookHandle;
            pCaptureMessage = !detaching;
            activeCallbacks++;
        }
        try {
            if (pCaptureMessage
                    && pCode >= IConstants.HUION_POINTER_HOOK_MIN_CODE
                    && Pointer.nativeValue(pWParam) == IConstants.HUION_POINTER_HOOK_REMOVE_MESSAGE
                    && !stopping) {
                NativeMessage pMessage = new NativeMessage(pLParam);
                pMessage.read();
                if (isTargetWindow(pMessage.window)) {
                    capturePointerEvent(pMessage);
                }
            }
        } catch (RuntimeException e) {
            failure.compareAndSet(
                    null,
                    new DeviceException(IConstants.ERR_HUION_POINTER_EVENT, e));
        }
        try {
            return pWindowsPointerApi.CallNextHookEx(pHookHandle, pCode, pWParam, pLParam);
        } finally {
            synchronized (callbackLock) {
                activeCallbacks--;
                if (activeCallbacks == IConstants.NATIVE_ZERO) {
                    callbackLock.notifyAll();
                }
            }
        }
    }

    private boolean isTargetWindow(Pointer pMessageWindow) {
        Pointer pWindowHandle = windowHandle;
        return pWindowHandle != null
                && (Pointer.nativeValue(pWindowHandle) == Pointer.nativeValue(pMessageWindow)
                        || windowsPointerApi.IsChild(pWindowHandle, pMessageWindow));
    }

    private void capturePointerEvent(NativeMessage pMessage) {
        if (!isPointerMessage(pMessage.message)) {
            return;
        }
        int pPointerId = (int) (Pointer.nativeValue(pMessage.wParam) & IConstants.HUION_POINTER_ID_MASK);
        IntByReference pType = new IntByReference();
        boolean pTypeAvailable = windowsPointerApi.GetPointerType(pPointerId, pType);
        boolean pIsPen = pTypeAvailable && pType.getValue() == IConstants.HUION_POINTER_TYPE_PEN;
        boolean pMatchesLastPen = pPointerId == lastPenPointerId;
        if (!pIsPen && !pMatchesLastPen) {
            return;
        }

        PointerPenInfo pPenInfo = new PointerPenInfo();
        boolean pPenInfoAvailable = pIsPen
                && windowsPointerApi.GetPointerPenInfo(pPointerId, pPenInfo);
        WinPoint pPoint = getScreenPoint(pMessage, pPenInfo, pPenInfoAvailable);
        boolean pCoordinatesAvailable = (pMessage.message == IConstants.HUION_WM_POINTER_CAPTURE_CHANGED
                        && !pPenInfoAvailable)
                || windowsPointerApi.ScreenToClient(windowHandle, pPoint);
        Component pComponent = component;
        Window pWindow = window;
        double pScaleX = IConstants.HUION_POINTER_INVALID_SCALE;
        double pScaleY = IConstants.HUION_POINTER_INVALID_SCALE;
        double pComponentX = IConstants.HUION_COORDINATE_MIN;
        double pComponentY = IConstants.HUION_COORDINATE_MIN;
        int pComponentWidth = IConstants.HUION_COORDINATE_MIN;
        int pComponentHeight = IConstants.HUION_COORDINATE_MIN;
        if (pCoordinatesAvailable && pComponent != null && pWindow != null) {
            GraphicsConfiguration pConfiguration = pComponent.getGraphicsConfiguration();
            if (pConfiguration == null) {
                pCoordinatesAvailable = false;
            } else {
                AffineTransform pTransform = pConfiguration.getDefaultTransform();
                Point pComponentOffset = SwingUtilities.convertPoint(
                        pComponent,
                        IConstants.HUION_COORDINATE_MIN,
                        IConstants.HUION_COORDINATE_MIN,
                        pWindow);
                pScaleX = pTransform.getScaleX();
                pScaleY = pTransform.getScaleY();
                pComponentX = pComponentOffset.x;
                pComponentY = pComponentOffset.y;
                pComponentWidth = pComponent.getWidth();
                pComponentHeight = pComponent.getHeight();
            }
        } else {
            pCoordinatesAvailable = false;
        }
        int pPressure = pPenInfoAvailable
                ? pPenInfo.pressure
                : IConstants.HUION_POINTER_RAW_PRESSURE_MIN;
        boolean pPressureAvailable = pPenInfoAvailable
                && (pPenInfo.penMask & IConstants.HUION_POINTER_PRESSURE_MASK)
                        != IConstants.HUION_POINTER_FLAG_NOT_SET;
        boolean pInContact = pPenInfoAvailable
                && (pPenInfo.pointerInfo.pointerFlags & IConstants.HUION_POINTER_IN_CONTACT_FLAG)
                        != IConstants.HUION_POINTER_FLAG_NOT_SET;

        lastClientX = pPoint.x;
        lastClientY = pPoint.y;
        lastPenPointerId = pMessage.message == IConstants.HUION_WM_POINTER_UP
                || pMessage.message == IConstants.HUION_WM_POINTER_LEAVE
                || pMessage.message == IConstants.HUION_WM_POINTER_CAPTURE_CHANGED
                        ? IConstants.HUION_NO_ACTIVE_POINTER
                        : pPointerId;
        enqueueEvent(new NativePointerEvent(
                pMessage.message,
                pPressure,
                pPoint.x,
                pPoint.y,
                pPressureAvailable,
                pInContact,
                pCoordinatesAvailable,
                pScaleX,
                pScaleY,
                pComponentX,
                pComponentY,
                pComponentWidth,
                pComponentHeight));
    }

    private WinPoint getScreenPoint(
            NativeMessage pMessage,
            PointerPenInfo pPenInfo,
            boolean pPenInfoAvailable) {
        if (pPenInfoAvailable) {
            return new WinPoint(
                    pPenInfo.pointerInfo.pixelLocation.x,
                    pPenInfo.pointerInfo.pixelLocation.y);
        }
        if (pMessage.message == IConstants.HUION_WM_POINTER_LEAVE) {
            long pLocation = Pointer.nativeValue(pMessage.lParam);
            return new WinPoint(
                    (short) (pLocation & IConstants.HUION_POINTER_COORDINATE_MASK),
                    (short) ((pLocation >>> IConstants.HUION_POINTER_COORDINATE_SHIFT)
                            & IConstants.HUION_POINTER_COORDINATE_MASK));
        }
        if (pMessage.message == IConstants.HUION_WM_POINTER_CAPTURE_CHANGED) {
            return new WinPoint(lastClientX, lastClientY);
        }
        return new WinPoint(pMessage.point.x, pMessage.point.y);
    }

    private void enqueueEvent(NativePointerEvent pEvent) {
        if (events.offer(pEvent)) {
            return;
        }
        if (pEvent.message() == IConstants.HUION_WM_POINTER_UPDATE) {
            return;
        }
        failure.compareAndSet(null, new DeviceException(IConstants.ERR_HUION_POINTER_EVENT));
        stopping = true;
    }

    private static boolean isPointerMessage(int pMessage) {
        return pMessage == IConstants.HUION_WM_POINTER_DOWN
                || pMessage == IConstants.HUION_WM_POINTER_UPDATE
                || pMessage == IConstants.HUION_WM_POINTER_UP
                || pMessage == IConstants.HUION_WM_POINTER_LEAVE
                || pMessage == IConstants.HUION_WM_POINTER_CAPTURE_CHANGED;
    }

    private static void ensureSupportedPlatform() throws DeviceException {
        String pOsName = System.getProperty(IConstants.OS_NAME_PROPERTY);
        String pArchitecture = System.getProperty(IConstants.OS_ARCH_PROPERTY);
        if (pOsName == null
                || !pOsName.toLowerCase(java.util.Locale.ROOT)
                        .startsWith(IConstants.HUION_POINTER_OS_WINDOWS)
                || !IConstants.HUION_POINTER_ARCH_AMD64.equalsIgnoreCase(pArchitecture)) {
            throw new DeviceException(IConstants.ERR_HUION_POINTER_NOT_WINDOWS);
        }
    }

    private static WindowsPointerApi getWindowsPointerApi() throws DeviceException {
        try {
            return WindowsPointerApiHolder.INSTANCE;
        } catch (UnsatisfiedLinkError e) {
            throw new DeviceException(IConstants.ERR_HUION_POINTER_API, e);
        }
    }

    private static class WindowsPointerApiHolder {

        private static final WindowsPointerApi INSTANCE = Native.load(
                IConstants.HUION_POINTER_LIBRARY,
                WindowsPointerApi.class,
                Map.of(Library.OPTION_FUNCTION_MAPPER,
                        (FunctionMapper) (pLibrary, pMethod) -> pMethod.getName()));
    }

    private interface PointerHook extends StdCallLibrary.StdCallCallback {

        Pointer callback(int pCode, Pointer pWParam, Pointer pLParam);
    }

    private interface WindowsPointerApi extends StdCallLibrary {

        boolean GetPointerType(int pPointerId, IntByReference pPointerType);

        boolean GetPointerPenInfo(int pPointerId, PointerPenInfo pPenInfo);

        int GetWindowThreadProcessId(Pointer pWindow, IntByReference pProcessId);

        boolean IsChild(Pointer pParent, Pointer pChild);

        boolean ScreenToClient(Pointer pWindow, WinPoint pPoint);

        Pointer SetWindowsHookExW(
                int pHookType,
                PointerHook pHookProcedure,
                Pointer pModule,
                int pThreadId);

        boolean UnhookWindowsHookEx(Pointer pHook);

        Pointer CallNextHookEx(
                Pointer pHook,
                int pCode,
                Pointer pWParam,
                Pointer pLParam);
    }

    @Structure.FieldOrder({
        "window",
        "message",
        "wParam",
        "lParam",
        "time",
        "point"
    })
    public static final class NativeMessage extends Structure {

        public Pointer window;
        public int message;
        public Pointer wParam;
        public Pointer lParam;
        public int time;
        public WinPoint point = new WinPoint();

        private NativeMessage(Pointer pPointer) {
            super(pPointer);
        }
    }

    @Structure.FieldOrder({
        "pointerInfo",
        "penFlags",
        "penMask",
        "pressure",
        "rotation",
        "tiltX",
        "tiltY"
    })
    public static final class PointerPenInfo extends Structure {

        public PointerInfo pointerInfo = new PointerInfo();
        public int penFlags;
        public int penMask;
        public int pressure;
        public int rotation;
        public int tiltX;
        public int tiltY;
    }

    @Structure.FieldOrder({
        "pointerType",
        "pointerId",
        "frameId",
        "pointerFlags",
        "sourceDevice",
        "targetWindow",
        "pixelLocation",
        "himetricLocation",
        "pixelLocationRaw",
        "himetricLocationRaw",
        "time",
        "historyCount",
        "inputData",
        "keyStates",
        "performanceCount",
        "buttonChangeType"
    })
    public static final class PointerInfo extends Structure implements Structure.ByValue {

        public int pointerType;
        public int pointerId;
        public int frameId;
        public int pointerFlags;
        public Pointer sourceDevice;
        public Pointer targetWindow;
        public WinPoint pixelLocation = new WinPoint();
        public WinPoint himetricLocation = new WinPoint();
        public WinPoint pixelLocationRaw = new WinPoint();
        public WinPoint himetricLocationRaw = new WinPoint();
        public int time;
        public int historyCount;
        public int inputData;
        public int keyStates;
        public long performanceCount;
        public int buttonChangeType;
    }

    @Structure.FieldOrder({"x", "y"})
    public static final class WinPoint extends Structure implements Structure.ByValue {

        public int x;
        public int y;

        public WinPoint() {
        }

        private WinPoint(int pX, int pY) {
            x = pX;
            y = pY;
        }
    }
}

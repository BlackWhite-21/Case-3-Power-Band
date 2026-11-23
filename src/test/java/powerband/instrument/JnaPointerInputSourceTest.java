package powerband.instrument;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

import powerband.exception.DeviceException;
import powerband.util.IConstants;

class JnaPointerInputSourceTest {

    @Test
    void retainsCallbackUntilUnhookAndActiveCallbacksFinish() throws Exception {
        FakeWindowsPointerApi pApi = new FakeWindowsPointerApi();
        JnaPointerInputSource pSource = createSource(pApi);
        pSource.installHook(pApi, null, null, new Pointer(Long.BYTES), Integer.BYTES);
        JnaPointerInputSource.PointerHook pCallback = pApi.callbackReference.get();
        assertSame(pCallback, getHookProcedure(pSource));

        pApi.blockNextCallback = true;
        Thread pCallbackThread = Thread.ofPlatform().start(() -> pCallback.callback(
                IConstants.HUION_POINTER_HOOK_MIN_CODE - Integer.BYTES,
                Pointer.NULL,
                Pointer.NULL));
        CompletableFuture<Void> pDetach = null;
        try {
            assertTrue(pApi.callNextEntered.await(2, TimeUnit.SECONDS));
            pDetach = CompletableFuture.runAsync(() -> {
                try {
                    pSource.detach();
                } catch (DeviceException pException) {
                    throw new CompletionException(pException);
                }
            });

            assertTrue(pApi.unhookEntered.await(2, TimeUnit.SECONDS));
            assertFalse(pDetach.isDone());
            assertSame(pCallback, getHookProcedure(pSource));
        } finally {
            pApi.allowCallbackReturn.countDown();
            pCallbackThread.join(TimeUnit.SECONDS.toMillis(2));
        }

        assertFalse(pCallbackThread.isAlive());
        pDetach.get(2, TimeUnit.SECONDS);
        assertNull(getHookProcedure(pSource));
    }

    @Test
    void retainsCallbackWhenUnhookFails() throws Exception {
        FakeWindowsPointerApi pApi = new FakeWindowsPointerApi();
        pApi.unhookSucceeds = false;
        JnaPointerInputSource pSource = createSource(pApi);
        pSource.installHook(pApi, null, null, new Pointer(Long.BYTES), Integer.BYTES);
        JnaPointerInputSource.PointerHook pCallback = pApi.callbackReference.get();

        assertThrows(DeviceException.class, pSource::detach);

        assertSame(pCallback, getHookProcedure(pSource));
    }

    private static JnaPointerInputSource createSource(FakeWindowsPointerApi pApi) {
        return new JnaPointerInputSource(pApi);
    }

    private static JnaPointerInputSource.PointerHook getHookProcedure(
            JnaPointerInputSource pSource) throws ReflectiveOperationException {
        Field pField = JnaPointerInputSource.class.getDeclaredField("hookProcedure");
        pField.setAccessible(true);
        return (JnaPointerInputSource.PointerHook) pField.get(pSource);
    }

    private static final class FakeWindowsPointerApi
            implements JnaPointerInputSource.WindowsPointerApi {

        private final CountDownLatch callNextEntered = new CountDownLatch(1);
        private final CountDownLatch allowCallbackReturn = new CountDownLatch(1);
        private final CountDownLatch unhookEntered = new CountDownLatch(1);
        private volatile WeakReference<JnaPointerInputSource.PointerHook> callbackReference =
                new WeakReference<>(null);
        private volatile boolean blockNextCallback;
        private volatile boolean unhookSucceeds = true;

        @Override
        public boolean GetPointerType(int pPointerId, IntByReference pPointerType) {
            return false;
        }

        @Override
        public boolean GetPointerPenInfo(
                int pPointerId,
                JnaPointerInputSource.PointerPenInfo pPenInfo) {
            return false;
        }

        @Override
        public int GetWindowThreadProcessId(Pointer pWindow, IntByReference pProcessId) {
            return IConstants.NATIVE_ZERO;
        }

        @Override
        public boolean IsChild(Pointer pParent, Pointer pChild) {
            return false;
        }

        @Override
        public boolean ScreenToClient(
                Pointer pWindow,
                JnaPointerInputSource.WinPointReference pPoint) {
            return false;
        }

        @Override
        public Pointer SetWindowsHookExW(
                int pHookType,
                JnaPointerInputSource.PointerHook pHookProcedure,
                Pointer pModule,
                int pThreadId) {
            callbackReference = new WeakReference<>(pHookProcedure);
            return new Pointer(Long.BYTES);
        }

        @Override
        public boolean UnhookWindowsHookEx(Pointer pHook) {
            unhookEntered.countDown();
            return unhookSucceeds;
        }

        @Override
        public Pointer CallNextHookEx(
                Pointer pHook,
                int pCode,
                Pointer pWParam,
                Pointer pLParam) {
            callNextEntered.countDown();
            if (blockNextCallback) {
                try {
                    allowCallbackReturn.await();
                } catch (InterruptedException pException) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(pException);
                }
            }
            return Pointer.NULL;
        }
    }
}

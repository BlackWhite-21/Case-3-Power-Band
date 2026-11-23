package powerband.instrument;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.EventQueue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

import powerband.dto.DatosTableta;
import powerband.dto.EstadoPluma;
import powerband.exception.DeviceException;
import powerband.util.IConstants;

class TabletaGraficaTest {

    @Test
    void dispatchesEventsOffTheEdtAndReleasesResourcesOnClose() throws Exception {
        FakePointerEventSource pSource = new FakePointerEventSource();
        try (TabletaGrafica pTablet = new TabletaGrafica(pSource)) {
        CountDownLatch pReceived = new CountDownLatch(1);
        CountDownLatch pReleased = new CountDownLatch(1);
        AtomicBoolean pCalledOnEdt = new AtomicBoolean(true);
        pTablet.addListener(pData -> {
            pCalledOnEdt.set(SwingUtilities.isEventDispatchThread());
            pReceived.countDown();
            if (pData.estado() == EstadoPluma.LIBERADO) {
                pReleased.countDown();
            }
        });

        pTablet.initialize(new DisplayableComponent()).get(2, TimeUnit.SECONDS);
        pSource.publish(new DatosTableta(10, 20, 0.4f, EstadoPluma.PRESIONADO));

        assertTrue(pReceived.await(2, TimeUnit.SECONDS));
        assertFalse(pCalledOnEdt.get());
        pTablet.closeAsync().get(2, TimeUnit.SECONDS);
        assertTrue(pReleased.await(2, TimeUnit.SECONDS));
        assertTrue(pSource.detached.get());
        }
    }

    @Test
    void listenerFailureDoesNotPreventDeliveryToOtherListeners() throws Exception {
        FakePointerEventSource pSource = new FakePointerEventSource();
        try (TabletaGrafica pTablet = new TabletaGrafica(pSource)) {
        CountDownLatch pReceived = new CountDownLatch(1);
        pTablet.addListener(pData -> {
            throw new IllegalStateException("listener failed");
        });
        pTablet.addListener(pData -> pReceived.countDown());

        pTablet.initialize(new DisplayableComponent()).get(2, TimeUnit.SECONDS);
        pSource.publish(new DatosTableta(10, 20, 0.4f, EstadoPluma.PRESIONADO));

        assertTrue(pReceived.await(2, TimeUnit.SECONDS));
        assertTrue(pTablet.getFailure().isEmpty());
        pTablet.closeAsync().get(2, TimeUnit.SECONDS);
        }
    }

    @Test
    void closingDoesNotBlockTheEventThreadWhileDetaching() throws Exception {
        FakePointerEventSource pSource = new FakePointerEventSource();
        pSource.blockDetach = true;
        try (TabletaGrafica pTablet = new TabletaGrafica(pSource)) {
        pTablet.initialize(new DisplayableComponent()).get(2, TimeUnit.SECONDS);

        CompletableFuture<CompletableFuture<Void>> pCloseCall = new CompletableFuture<>();
        EventQueue.invokeLater(() -> pCloseCall.complete(pTablet.closeAsync()));
        CompletableFuture<Void> pTermination;
        try {
            pTermination = pCloseCall.get(2, TimeUnit.SECONDS);
            assertTrue(pSource.detachEntered.await(2, TimeUnit.SECONDS));
            CountDownLatch pEventThreadResponsive = new CountDownLatch(1);
            EventQueue.invokeLater(pEventThreadResponsive::countDown);
            assertTrue(pEventThreadResponsive.await(2, TimeUnit.SECONDS));
        } finally {
            pSource.allowDetach.countDown();
        }

        pTermination.get(2, TimeUnit.SECONDS);
        }
    }

    @Test
    void reportsAsynchronousCaptureFailuresToListeners() throws Exception {
        FakePointerEventSource pSource = new FakePointerEventSource();
        try (TabletaGrafica pTablet = new TabletaGrafica(pSource)) {
        CountDownLatch pFailed = new CountDownLatch(1);
        AtomicReference<DeviceException> pReported = new AtomicReference<>();
        pTablet.addListener(new TabletaListener() {
            @Override
            public void onDatosTableta(DatosTableta pData) {
            }

            @Override
            public void onError(DeviceException pException) {
                pReported.set(pException);
                pFailed.countDown();
            }
        });
        pSource.failNextPoll = true;

        pTablet.initialize(new DisplayableComponent()).get(2, TimeUnit.SECONDS);

        assertTrue(pFailed.await(2, TimeUnit.SECONDS));
        assertEquals(pReported.get(), pTablet.getFailure().orElseThrow());
        pTablet.closeAsync().get(2, TimeUnit.SECONDS);
        }
    }

    @Test
    void rejectsNonDisplayableComponentsAndRepeatedInitialization() throws Exception {
        FakePointerEventSource pSource = new FakePointerEventSource();
        try (TabletaGrafica pTablet = new TabletaGrafica(pSource)) {
            DeviceException pFirstFailure = assertThrows(
                    DeviceException.class,
                    () -> pTablet.initialize(new Component() {
                    }));
            assertEquals(IConstants.ERR_HUION_POINTER_NOT_DISPLAYABLE, pFirstFailure.getMessage());

            pTablet.initialize(new DisplayableComponent()).get(2, TimeUnit.SECONDS);
            DeviceException pRepeatedFailure = assertThrows(
                    DeviceException.class,
                    () -> pTablet.initialize(new DisplayableComponent()));
            assertEquals(IConstants.ERR_HUION_POINTER_ALREADY_STARTED, pRepeatedFailure.getMessage());
            pTablet.closeAsync().get(2, TimeUnit.SECONDS);
        }
    }

    @Test
    void rejectsUnsupportedOsBeforeLoadingWindowsApi() throws Exception {
        String pOriginalOsName = System.getProperty(IConstants.OS_NAME_PROPERTY);
        AtomicReference<DeviceException> pFailure = new AtomicReference<>();
        try {
            System.setProperty(IConstants.OS_NAME_PROPERTY, "Linux");
            SwingUtilities.invokeAndWait(() -> {
                try {
                    new JnaPointerInputSource().attach(new Component() {
                    });
                } catch (DeviceException e) {
                    pFailure.set(e);
                }
            });
        } finally {
            if (pOriginalOsName == null) {
                System.clearProperty(IConstants.OS_NAME_PROPERTY);
            } else {
                System.setProperty(IConstants.OS_NAME_PROPERTY, pOriginalOsName);
            }
        }

        assertEquals(IConstants.ERR_HUION_POINTER_NOT_WINDOWS, pFailure.get().getMessage());
    }

    private static final class FakePointerEventSource implements PointerEventSource {

        private final ArrayBlockingQueue<DatosTableta> events = new ArrayBlockingQueue<>(4);
        private final AtomicBoolean detached = new AtomicBoolean();
        private final CountDownLatch detachEntered = new CountDownLatch(1);
        private final CountDownLatch allowDetach = new CountDownLatch(1);
        private volatile boolean failNextPoll;
        private volatile boolean blockDetach;

        @Override
        public void attach(Component pComponent) {
        }

        @Override
        public DatosTableta pollEvent(int pTimeoutMs) throws DeviceException {
            if (failNextPoll) {
                failNextPoll = false;
                throw new DeviceException("fake capture failure");
            }
            try {
                return events.poll(pTimeoutMs, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new DeviceException("fake poll interrupted", e);
            }
        }

        @Override
        public void requestStop() {
        }

        @Override
        public void detach() throws DeviceException {
            detached.set(true);
            detachEntered.countDown();
            if (blockDetach) {
                try {
                    if (!allowDetach.await(2, TimeUnit.SECONDS)) {
                        throw new DeviceException("fake detach timed out");
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new DeviceException("fake detach interrupted", e);
                }
            }
        }

        private void publish(DatosTableta pData) throws InterruptedException {
            events.offer(pData, 1, TimeUnit.SECONDS);
        }
    }

    private static final class DisplayableComponent extends Component {

        @Override
        public boolean isDisplayable() {
            return true;
        }

        @Override
        public boolean isShowing() {
            return true;
        }

        @Override
        public int getWidth() {
            return 100;
        }

        @Override
        public int getHeight() {
            return 100;
        }
    }
}

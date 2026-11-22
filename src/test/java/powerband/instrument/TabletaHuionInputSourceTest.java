package powerband.instrument;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import powerband.dto.DatosTableta;
import powerband.dto.EstadoPluma;
import powerband.dto.TabletReading;
import powerband.exception.DeviceException;

class TabletaHuionInputSourceTest {

    @Test
    void normalizesCoordinatesAndReleasesPressureWhenPenLeavesContact() throws Exception {
        FakePointerEventSource pPointerSource = new FakePointerEventSource();
        TabletaGrafica pTablet = new TabletaGrafica(pPointerSource);
        TabletaHuionInputSource pInputSource =
                new TabletaHuionInputSource(new DisplayableComponent(), pTablet);
        ArrayBlockingQueue<TabletReading> pReadings = new ArrayBlockingQueue<>(2);
        AtomicReference<DeviceException> pFailure = new AtomicReference<>();
        AtomicBoolean pCalledOnEdt = new AtomicBoolean(true);
        CountDownLatch pReadingsReceived = new CountDownLatch(2);

        pInputSource.start(pReading -> {
            pCalledOnEdt.set(javax.swing.SwingUtilities.isEventDispatchThread());
            pReadings.offer(pReading);
            pReadingsReceived.countDown();
        }, pFailure::set);
        pPointerSource.publish(new DatosTableta(50, 25, 0.75f, EstadoPluma.PRESIONADO));
        pPointerSource.publish(new DatosTableta(50, 25, 0.75f, EstadoPluma.LIBERADO));

        assertTrue(pReadingsReceived.await(2, TimeUnit.SECONDS));
        TabletReading pPressed = pReadings.poll();
        TabletReading pReleased = pReadings.poll();
        assertEquals(0.25f, pPressed.x(), 0.0001f);
        assertEquals(0.25f, pPressed.y(), 0.0001f);
        assertEquals(0.75f, pPressed.pressure(), 0.0001f);
        assertEquals(0f, pReleased.pressure());
        assertFalse(pCalledOnEdt.get());
        assertEquals(null, pFailure.get());

        pInputSource.stop();
        assertTrue(pPointerSource.detached.await(2, TimeUnit.SECONDS));
    }

    @Test
    void forwardsAsynchronousPointerFailures() throws Exception {
        FakePointerEventSource pPointerSource = new FakePointerEventSource();
        pPointerSource.failNextPoll = true;
        TabletaHuionInputSource pInputSource = new TabletaHuionInputSource(
                new DisplayableComponent(),
                new TabletaGrafica(pPointerSource));
        AtomicReference<DeviceException> pFailure = new AtomicReference<>();
        CountDownLatch pFailureReceived = new CountDownLatch(1);
        DeviceException pExpectedFailure = new DeviceException("fake pointer failure");

        pPointerSource.nextFailure = pExpectedFailure;
        pInputSource.start(pReading -> {
        }, pException -> {
            pFailure.set(pException);
            pFailureReceived.countDown();
        });

        assertTrue(pFailureReceived.await(2, TimeUnit.SECONDS));
        assertSame(pExpectedFailure, pFailure.get());
        pInputSource.stop();
        assertTrue(pPointerSource.detached.await(2, TimeUnit.SECONDS));
    }

    private static final class FakePointerEventSource implements PointerEventSource {

        private final ArrayBlockingQueue<DatosTableta> events = new ArrayBlockingQueue<>(2);
        private final CountDownLatch detached = new CountDownLatch(1);
        private volatile boolean failNextPoll;
        private volatile DeviceException nextFailure;

        @Override
        public void attach(Component pComponent) {
        }

        @Override
        public DatosTableta pollEvent(int pTimeoutMs) throws DeviceException {
            if (failNextPoll) {
                failNextPoll = false;
                throw nextFailure;
            }
            try {
                return events.poll(pTimeoutMs, TimeUnit.MILLISECONDS);
            } catch (InterruptedException pException) {
                Thread.currentThread().interrupt();
                throw new DeviceException("fake pointer polling interrupted", pException);
            }
        }

        @Override
        public void requestStop() {
        }

        @Override
        public void detach() {
            detached.countDown();
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
            return 200;
        }

        @Override
        public int getHeight() {
            return 100;
        }
    }
}

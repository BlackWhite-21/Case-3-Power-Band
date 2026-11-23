package powerband;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import powerband.exception.DeviceException;
import powerband.util.IConstants;

class MainTest {

    @Test
    void reportsOneInstrumentFailureWithoutPreventingAnotherFromStarting() {
        Object pLifecycleLock = new Object();
        AtomicBoolean pShutdown = new AtomicBoolean();
        AtomicReference<DeviceException> pReportedFailure = new AtomicReference<>();
        AtomicInteger pStartedInstruments = new AtomicInteger();
        DeviceException pExpectedFailure = new DeviceException(IConstants.ERR_MIC_OPEN);

        Main.startInstrument(
                () -> {
                    throw pExpectedFailure;
                },
                pShutdown,
                pLifecycleLock,
                pReportedFailure::set);
        Main.startInstrument(
                pStartedInstruments::incrementAndGet,
                pShutdown,
                pLifecycleLock,
                pReportedFailure::set);

        assertSame(pExpectedFailure, pReportedFailure.get());
        assertEquals(1, pStartedInstruments.get());
    }

    @Test
    void doesNotStartAnInstrumentAfterShutdownWasRequested() {
        AtomicInteger pStartedInstruments = new AtomicInteger();

        Main.startInstrument(
                pStartedInstruments::incrementAndGet,
                new AtomicBoolean(true),
                new Object(),
                pException -> {
                });

        assertEquals(0, pStartedInstruments.get());
    }
}

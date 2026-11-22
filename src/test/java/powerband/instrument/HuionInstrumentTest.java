package powerband.instrument;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static powerband.util.IConstants.SYNTH_SAXOPHONE_ID;
import static powerband.util.IConstants.ACTION_OFF;
import static powerband.util.IConstants.ACTION_ON;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import powerband.dto.SynthEvent;
import powerband.dto.TabletReading;
import powerband.exception.DeviceException;
import powerband.helpers.FakeOscTransport;
import powerband.osc.OscClient;
import powerband.synth.SaxophoneCommand;

class HuionInstrumentTest {

    @Test
    void dispatchesTabletEventsAndReleasesNoteWhenStopped() throws Exception {
        FakeHuionInputSource source = new FakeHuionInputSource();
        FakeOscTransport transport = new FakeOscTransport();
        List<SynthEvent> events = new ArrayList<>();
        HuionInstrument instrument = new HuionInstrument(
                source, new SaxophoneCommand(new OscClient(transport)), events::add);

        instrument.start();
        source.publish(new TabletReading(0.5f, 0f, 0.7f));
        instrument.stop();

        assertEquals(2, events.size());
        assertEquals(SYNTH_SAXOPHONE_ID, events.getFirst().synthId());
        assertEquals(2, transport.getSent().size());
        assertTrue(transport.lastAsText().startsWith("/n_set"));
        assertTrue(source.stopped);
    }

    @Test
    void releasesActiveNoteAndReportsAsynchronousInputFailure() throws Exception {
        FakeHuionInputSource pSource = new FakeHuionInputSource();
        FakeOscTransport pTransport = new FakeOscTransport();
        List<SynthEvent> pEvents = new ArrayList<>();
        AtomicReference<DeviceException> pFailure = new AtomicReference<>();
        HuionInstrument pInstrument = new HuionInstrument(
                pSource,
                new SaxophoneCommand(new OscClient(pTransport)),
                pEvents::add,
                pFailure::set);

        pInstrument.start();
        pSource.publish(new TabletReading(0.5f, 0f, 0.7f));
        DeviceException pExpectedFailure = new DeviceException("input lost");
        pSource.fail(pExpectedFailure);

        assertEquals(2, pEvents.size());
        assertEquals(ACTION_ON, pEvents.getFirst().action());
        assertEquals(ACTION_OFF, pEvents.getLast().action());
        assertEquals(pExpectedFailure, pFailure.get());
    }

    private static final class FakeHuionInputSource implements HuionInputSource {

        private Consumer<TabletReading> readingConsumer;
        private Consumer<DeviceException> failureConsumer;
        private boolean stopped;

        @Override
        public void start(
                Consumer<TabletReading> pReadingConsumer,
                Consumer<DeviceException> pFailureConsumer) {
            readingConsumer = pReadingConsumer;
            failureConsumer = pFailureConsumer;
        }

        @Override
        public void stop() {
            stopped = true;
        }

        private void publish(TabletReading pReading) {
            readingConsumer.accept(pReading);
        }

        private void fail(DeviceException pException) {
            failureConsumer.accept(pException);
        }
    }
}

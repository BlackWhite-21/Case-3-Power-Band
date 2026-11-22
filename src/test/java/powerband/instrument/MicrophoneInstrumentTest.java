package powerband.instrument;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static powerband.util.IConstants.ACTION_OFF;
import static powerband.util.IConstants.ACTION_ON;
import static powerband.util.IConstants.MIC_FRAME_SAMPLES;
import static powerband.util.IConstants.MIC_SAMPLE_RATE;
import static powerband.util.IConstants.MIDI_A4_FREQ;
import static powerband.util.IConstants.SYNTH_VIOLIN_ID;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import powerband.dto.SynthEvent;
import powerband.exception.DeviceException;
import powerband.exception.PowerBandException;
import powerband.helpers.FakeOscTransport;
import powerband.osc.OscClient;
import powerband.synth.ViolinCommand;

class MicrophoneInstrumentTest {

    @Test
    void usesInjectedAudioSourceAndDispatchesOnAndOffWithoutHardware() throws Exception {
        BlockingAudioSource source = new BlockingAudioSource();
        List<SynthEvent> events = new CopyOnWriteArrayList<>();
        List<PowerBandException> failures = new CopyOnWriteArrayList<>();
        CountDownLatch noteOn = new CountDownLatch(1);
        CountDownLatch noteOff = new CountDownLatch(1);
        FakeOscTransport transport = new FakeOscTransport();
        MicrophoneInstrument instrument = new MicrophoneInstrument(
                source,
                new ViolinCommand(new OscClient(transport)),
                event -> {
                    events.add(event);
                    if (event.action() == ACTION_ON) {
                        noteOn.countDown();
                    } else if (event.action() == ACTION_OFF) {
                        noteOff.countDown();
                    }
                },
                failures::add);

        instrument.start();
        boolean receivedOn = noteOn.await(3, TimeUnit.SECONDS);
        instrument.stop();
        boolean receivedOff = noteOff.await(3, TimeUnit.SECONDS);

        assertTrue(receivedOn);
        assertTrue(receivedOff);
        assertTrue(failures.isEmpty());
        assertEquals(2, events.size());
        assertEquals(SYNTH_VIOLIN_ID, events.getFirst().synthId());
        assertEquals(2, transport.getSent().size());
        assertTrue(transport.lastAsText().startsWith("/n_set"));
    }

    private static final class BlockingAudioSource implements MicrophoneAudioSource {

        private boolean closed;
        private boolean firstFrame = true;

        @Override
        public void open() {
            closed = false;
        }

        @Override
        public synchronized float[] readFrame() throws DeviceException {
            if (firstFrame) {
                firstFrame = false;
                return a4Frame();
            }
            while (!closed) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new DeviceException("interrupted", e);
                }
            }
            return new float[MIC_FRAME_SAMPLES];
        }

        @Override
        public synchronized void close() {
            closed = true;
            notifyAll();
        }

        private float[] a4Frame() {
            float[] samples = new float[MIC_FRAME_SAMPLES];
            for (int index = 0; index < samples.length; index++) {
                samples[index] = (float) (0.25 * Math.sin(
                        2.0 * Math.PI * MIDI_A4_FREQ * index / MIC_SAMPLE_RATE));
            }
            return samples;
        }
    }
}

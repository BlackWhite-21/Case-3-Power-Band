package powerband.instrument;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static powerband.util.IConstants.ACTION_OFF;
import static powerband.util.IConstants.ACTION_ON;
import static powerband.util.IConstants.JOY_BASE_NOTE;
import static powerband.util.IConstants.SYNTH_ORGAN_ID;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.junit.jupiter.api.Test;

import powerband.dto.JoystickState;
import powerband.dto.SynthEvent;
import powerband.exception.DeviceException;
import powerband.helpers.FakeOscTransport;
import powerband.osc.OscClient;
import powerband.synth.OrganCommand;

class JoystickInstrumentTest {

    @Test
    void buttonPressAndReleaseDispatchOneOnAndOneOff() {
        FakeJoystickInputSource pSource = new FakeJoystickInputSource();
        List<SynthEvent> pEvents = new CopyOnWriteArrayList<>();
        JoystickInstrument pInstrument = new JoystickInstrument(
                pSource,
                new OrganCommand(new OscClient(new FakeOscTransport())),
                pEvents::add);

        pInstrument.accept(new JoystickState(0.25f, 0.50f, 0.75f, 1));
        pInstrument.accept(new JoystickState(0.25f, 0.50f, 0.75f, 1));
        pInstrument.accept(new JoystickState(0.25f, 0.50f, 0.75f, 0));

        assertEquals(2, pEvents.size());
        assertEquals(ACTION_ON, pEvents.getFirst().action());
        assertEquals(JOY_BASE_NOTE, pEvents.getFirst().note());
        assertEquals(SYNTH_ORGAN_ID, pEvents.getFirst().synthId());
        assertEquals(ACTION_OFF, pEvents.getLast().action());
    }

    @Test
    void axisChangesUpdateHeldButtonsWithoutExtraButtonOn() {
        FakeJoystickInputSource pSource = new FakeJoystickInputSource();
        List<SynthEvent> pEvents = new CopyOnWriteArrayList<>();
        JoystickInstrument pInstrument = new JoystickInstrument(
                pSource,
                new OrganCommand(new OscClient(new FakeOscTransport())),
                pEvents::add);

        pInstrument.accept(new JoystickState(0.10f, 0.20f, 0.30f, 1));
        pInstrument.accept(new JoystickState(0.40f, 0.50f, 0.60f, 1));

        assertEquals(2, pEvents.size());
        assertEquals(ACTION_ON, pEvents.getFirst().action());
        assertEquals(ACTION_ON, pEvents.getLast().action());
        assertEquals(0.40f, pEvents.getLast().timbre1(), 0.0001f);
        assertEquals(0.50f, pEvents.getLast().timbre2(), 0.0001f);
        assertEquals(0.60f, pEvents.getLast().timbre3(), 0.0001f);
    }

    @Test
    void failureListenerReceivesUnsupportedPlatformOrDeviceFailure() {
        List<DeviceException> pFailures = new CopyOnWriteArrayList<>();
        JoystickInstrument pInstrument = new JoystickInstrument(
                new FailingJoystickInputSource(),
                new OrganCommand(new OscClient(new FakeOscTransport())),
                pEvent -> {
                },
                pFailures::add,
                pState -> {
                });

        try {
            pInstrument.start();
        } catch (DeviceException pException) {
            pFailures.add(pException);
        }

        assertEquals(1, pFailures.size());
    }

    private static final class FakeJoystickInputSource implements JoystickInputSource {

        @Override
        public void open() {
        }

        @Override
        public JoystickState readState() {
            return new JoystickState(0f, 0f, 0f, 0);
        }

        @Override
        public void close() {
        }
    }

    private static final class FailingJoystickInputSource implements JoystickInputSource {

        @Override
        public void open() throws DeviceException {
            throw new DeviceException("joystick fake failure");
        }

        @Override
        public JoystickState readState() throws DeviceException {
            throw new DeviceException("joystick fake failure");
        }

        @Override
        public void close() {
        }
    }
}

package powerband.synth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static powerband.util.IConstants.ACTION_OFF;
import static powerband.util.IConstants.ACTION_ON;
import static powerband.util.IConstants.ACTION_PLAY;
import static powerband.util.IConstants.SYNTH_BASS_ID;
import static powerband.util.IConstants.SYNTH_ORGAN_ID;
import static powerband.util.IConstants.SYNTH_PIANO_ID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import powerband.dto.SynthEvent;
import powerband.exception.SynthException;
import powerband.helpers.FakeOscTransport;
import powerband.osc.OscClient;

class SynthCommandTest {

    private FakeOscTransport transport;
    private OscClient client;

    @BeforeEach
    void setUp() {
        transport = new FakeOscTransport();
        client = new OscClient(transport);
    }

    private SynthEvent event(int pSynth, int pAction, int pNote) {
        return new SynthEvent(0, pSynth, pAction, pNote, 500, 0.5f, 1f, 0.5f, 0.5f);
    }

    @Test
    void pianoSendsSNewWithClassicPiano() throws Exception {
        new PianoCommand(client).execute(event(SYNTH_PIANO_ID, ACTION_PLAY, 48));
        assertEquals(1, transport.getSent().size());
        assertTrue(transport.lastAsText().startsWith("/s_new"));
        assertTrue(transport.lastAsText().contains("classicPiano"));
        assertTrue(transport.lastAsText().contains("third"));
    }

    @Test
    void bassSendsSNewWithStringBass() throws Exception {
        new BassCommand(client).execute(event(SYNTH_BASS_ID, ACTION_PLAY, 36));
        assertTrue(transport.lastAsText().contains("stringBass"));
    }

    @Test
    void organNoteOffSendsGateZero() throws Exception {
        OrganCommand organ = new OrganCommand(client);
        organ.execute(event(SYNTH_ORGAN_ID, ACTION_ON, 60));
        assertTrue(transport.lastAsText().contains("organ70s"));
        organ.execute(event(SYNTH_ORGAN_ID, ACTION_OFF, 60));
        assertTrue(transport.lastAsText().startsWith("/n_set"));
        assertTrue(transport.lastAsText().contains("gate"));
    }

    @Test
    void organOffWithoutOnSendsNothing() throws Exception {
        new OrganCommand(client).execute(event(SYNTH_ORGAN_ID, ACTION_OFF, 60));
        assertEquals(0, transport.getSent().size());
    }

    @Test
    void pianoIgnoresNoteOffBecauseItStopsAlone() throws Exception {
        new PianoCommand(client).execute(event(SYNTH_PIANO_ID, ACTION_OFF, 48));
        assertEquals(0, transport.getSent().size());
    }

    @Test
    void registryRoutesEventBySynthId() throws Exception {
        SynthCommandRegistry registry = new SynthCommandRegistry();
        registry.register(new PianoCommand(client));
        registry.register(new BassCommand(client));
        registry.execute(event(SYNTH_BASS_ID, ACTION_PLAY, 40));
        assertTrue(transport.lastAsText().contains("stringBass"));
        assertThrows(SynthException.class, () -> registry.get(99));
    }

    @Test
    void pingFailsWhenServerDoesNotAnswer() {
        assertThrows(SynthException.class, () -> client.ping());
    }

    @Test
    void pingSucceedsWithStatusReply() throws Exception {
        transport.setReply("/status.reply");
        client.ping();
        assertTrue(transport.lastAsText().startsWith("/status"));
    }
}

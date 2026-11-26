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
import static powerband.util.IConstants.SYNTH_SAXOPHONE_ID;
import static powerband.util.IConstants.SYNTH_VIOLIN_ID;

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
    void organAxisUpdateDoesNotRetriggerActiveNote() throws Exception {
        OrganCommand pOrgan = new OrganCommand(client);
        pOrgan.execute(event(SYNTH_ORGAN_ID, ACTION_ON, 60));
        int pSentAfterOn = transport.getSent().size();
        pOrgan.execute(event(SYNTH_ORGAN_ID, ACTION_ON, 60));

        assertEquals(pSentAfterOn + 5, transport.getSent().size());
        assertTrue(transport.lastAsText().startsWith("/n_set"));
    }

    @Test
    void pianoIgnoresNoteOffBecauseItStopsAlone() throws Exception {
        new PianoCommand(client).execute(event(SYNTH_PIANO_ID, ACTION_OFF, 48));
        assertEquals(0, transport.getSent().size());
    }

    @Test
    void saxophoneUpdatesAmplitudeWithoutRetriggeringAndStopsWithGate() throws Exception {
        SaxophoneCommand saxophone = new SaxophoneCommand(client);
        saxophone.execute(event(SYNTH_SAXOPHONE_ID, ACTION_ON, 60));
        assertTrue(transport.lastAsText().contains("saxophone"));
        assertEquals(1, transport.getSent().size());

        saxophone.execute(new SynthEvent(0, SYNTH_SAXOPHONE_ID, ACTION_ON,
                60, 0, 0.9f, 0f, 0f, 0f));
        assertTrue(transport.lastAsText().startsWith("/n_set"));
        assertEquals(2, transport.getSent().size());

        saxophone.execute(event(SYNTH_SAXOPHONE_ID, ACTION_OFF, 60));
        assertTrue(transport.lastAsText().startsWith("/n_set"));
        assertTrue(transport.lastAsText().contains("gate"));
    }

    @Test
    void violinStartsAndStopsSustainedNote() throws Exception {
        ViolinCommand violin = new ViolinCommand(client);
        violin.execute(event(SYNTH_VIOLIN_ID, ACTION_ON, 69));
        assertTrue(transport.lastAsText().contains("violin"));

        violin.execute(event(SYNTH_VIOLIN_ID, ACTION_OFF, 69));
        assertTrue(transport.lastAsText().startsWith("/n_set"));
        assertTrue(transport.lastAsText().contains("gate"));
    }

    @Test
    void registryRoutesEventBySynthId() throws Exception {
        SynthCommandRegistry registry = new SynthCommandRegistry();
        registry.register(new PianoCommand(client));
        registry.register(new BassCommand(client));
        registry.register(new OrganCommand(client));
        registry.register(new SaxophoneCommand(client));
        registry.register(new ViolinCommand(client));
        registry.execute(event(SYNTH_BASS_ID, ACTION_PLAY, 40));
        assertTrue(transport.lastAsText().contains("stringBass"));
        registry.execute(event(SYNTH_VIOLIN_ID, ACTION_ON, 69));
        assertTrue(transport.lastAsText().contains("violin"));
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

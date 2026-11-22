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

import org.junit.jupiter.api.Test;

import powerband.dto.SynthEvent;

class MicrophonePitchAnalyzerTest {

    @Test
    void mapsA4ToneAndRmsToSustainedViolinEvent() {
        MicrophonePitchAnalyzer analyzer = new MicrophonePitchAnalyzer();

        SynthEvent event = analyzer.processFrame(a4Frame()).getFirst();

        assertEquals(SYNTH_VIOLIN_ID, event.synthId());
        assertEquals(ACTION_ON, event.action());
        assertEquals(69, event.note());
        assertTrue(event.velocity() > 0f);
        assertTrue(event.velocity() <= 1f);
    }

    @Test
    void silenceReleasesAnActiveNoteOnce() {
        MicrophonePitchAnalyzer analyzer = new MicrophonePitchAnalyzer();
        analyzer.processFrame(a4Frame());

        List<SynthEvent> release = analyzer.processFrame(new float[MIC_FRAME_SAMPLES]);

        assertEquals(1, release.size());
        assertEquals(ACTION_OFF, release.getFirst().action());
        assertEquals(69, release.getFirst().note());
        assertTrue(analyzer.processFrame(new float[MIC_FRAME_SAMPLES]).isEmpty());
    }

    @Test
    void ignoresQuietInputUntilOnsetThresholdIsCrossed() {
        MicrophonePitchAnalyzer analyzer = new MicrophonePitchAnalyzer();
        float[] quiet = a4Frame();
        for (int index = 0; index < quiet.length; index++) {
            quiet[index] *= 0.01f;
        }

        assertTrue(analyzer.processFrame(quiet).isEmpty());
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

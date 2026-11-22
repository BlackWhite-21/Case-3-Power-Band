package powerband.instrument;

import static powerband.util.IConstants.ACTION_OFF;
import static powerband.util.IConstants.ACTION_ON;
import static powerband.util.IConstants.MIC_ONSET_RMS;
import static powerband.util.IConstants.MIC_RELEASE_RMS;
import static powerband.util.IConstants.MIC_RMS_FULL;
import static powerband.util.IConstants.MIC_SAMPLE_RATE;
import static powerband.util.IConstants.MIDI_A4_FREQ;
import static powerband.util.IConstants.MIDI_A4_NOTE;
import static powerband.util.IConstants.MIDI_MAX_NOTE;
import static powerband.util.IConstants.MIDI_MIN_NOTE;
import static powerband.util.IConstants.NO_ACTIVE_NOTE;
import static powerband.util.IConstants.PITCH_MAX_FREQ;
import static powerband.util.IConstants.PITCH_MIN_CORRELATION;
import static powerband.util.IConstants.PITCH_MIN_FREQ;
import static powerband.util.IConstants.SEMITONES_PER_OCTAVE;
import static powerband.util.IConstants.SYNTH_VIOLIN_ID;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import powerband.dto.SynthEvent;

/**
 * Estimates a monophonic MIDI pitch and velocity from a PCM frame.
 */
public class MicrophonePitchAnalyzer {

    private int activeNote = NO_ACTIVE_NOTE;

    public synchronized List<SynthEvent> processFrame(float[] pSamples) {
        Objects.requireNonNull(pSamples);
        if (pSamples.length == 0) {
            return release();
        }

        double rms = rms(pSamples);
        if (activeNote == NO_ACTIVE_NOTE && rms < MIC_ONSET_RMS) {
            return List.of();
        }
        if (activeNote != NO_ACTIVE_NOTE && rms < MIC_RELEASE_RMS) {
            return release();
        }

        int note = estimateNote(pSamples);
        if (note == NO_ACTIVE_NOTE) {
            return release();
        }

        List<SynthEvent> events = new ArrayList<>();
        if (activeNote != NO_ACTIVE_NOTE && activeNote != note) {
            events.add(event(ACTION_OFF, activeNote, 0f));
        }
        events.add(event(ACTION_ON, note, (float) Math.clamp(rms / MIC_RMS_FULL, 0.0, 1.0)));
        activeNote = note;
        return List.copyOf(events);
    }

    public synchronized List<SynthEvent> release() {
        if (activeNote == NO_ACTIVE_NOTE) {
            return List.of();
        }
        SynthEvent event = event(ACTION_OFF, activeNote, 0f);
        activeNote = NO_ACTIVE_NOTE;
        return List.of(event);
    }

    private double rms(float[] pSamples) {
        double sumSquares = 0.0;
        for (float sample : pSamples) {
            sumSquares += sample * sample;
        }
        return Math.sqrt(sumSquares / pSamples.length);
    }

    private int estimateNote(float[] pSamples) {
        int minLag = (int) (MIC_SAMPLE_RATE / PITCH_MAX_FREQ);
        int maxLag = Math.min(pSamples.length / 2, (int) (MIC_SAMPLE_RATE / PITCH_MIN_FREQ));
        if (maxLag <= minLag) {
            return NO_ACTIVE_NOTE;
        }
        double[] correlations = new double[maxLag + 1];

        for (int lag = minLag; lag <= maxLag; lag++) {
            correlations[lag] = normalizedCorrelation(pSamples, lag);
        }

        int bestLag = NO_ACTIVE_NOTE;
        for (int lag = minLag + 1; lag < maxLag; lag++) {
            boolean localPeak = correlations[lag] >= correlations[lag - 1]
                    && correlations[lag] > correlations[lag + 1];
            if (localPeak && correlations[lag] >= PITCH_MIN_CORRELATION) {
                bestLag = lag;
                break;
            }
        }
        if (bestLag == NO_ACTIVE_NOTE) {
            return NO_ACTIVE_NOTE;
        }
        double frequency = MIC_SAMPLE_RATE / bestLag;
        int note = (int) Math.round(MIDI_A4_NOTE
                + SEMITONES_PER_OCTAVE * Math.log(frequency / MIDI_A4_FREQ) / Math.log(2.0));
        return Math.clamp(note, MIDI_MIN_NOTE, MIDI_MAX_NOTE);
    }

    private double normalizedCorrelation(float[] pSamples, int pLag) {
        double cross = 0.0;
        double leftEnergy = 0.0;
        double rightEnergy = 0.0;
        for (int index = 0; index < pSamples.length - pLag; index++) {
            double left = pSamples[index];
            double right = pSamples[index + pLag];
            cross += left * right;
            leftEnergy += left * left;
            rightEnergy += right * right;
        }
        double denominator = Math.sqrt(leftEnergy * rightEnergy);
        return denominator == 0.0 ? 0.0 : cross / denominator;
    }

    private SynthEvent event(int pAction, int pNote, float pVelocity) {
        return new SynthEvent(0L, SYNTH_VIOLIN_ID, pAction, pNote, 0,
                pVelocity, 0f, 0f, 0f);
    }
}

package powerband.instrument;

import static powerband.util.IConstants.ACTION_OFF;
import static powerband.util.IConstants.ACTION_ON;
import static powerband.util.IConstants.HUION_BASE_NOTE;
import static powerband.util.IConstants.HUION_OCTAVE_COUNT;
import static powerband.util.IConstants.NOTES_PER_OCTAVE;
import static powerband.util.IConstants.NO_ACTIVE_NOTE;
import static powerband.util.IConstants.SYNTH_SAXOPHONE_ID;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import powerband.dto.SynthEvent;
import powerband.dto.TabletReading;

/**
 * Maps normalized tablet coordinates and pressure to sustained MIDI note events.
 */
public class HuionInputMapper {

    private int activeNote = NO_ACTIVE_NOTE;

    public synchronized List<SynthEvent> map(TabletReading pReading) {
        Objects.requireNonNull(pReading);
        List<SynthEvent> events = new ArrayList<>();
        float pressure = normalize(pReading.pressure());

        if (pressure <= 0f) {
            releaseActiveNote(events);
            return List.copyOf(events);
        }

        int note = noteFor(pReading.x(), pReading.y());
        if (activeNote != NO_ACTIVE_NOTE && activeNote != note) {
            events.add(event(ACTION_OFF, activeNote, 0f));
        }
        events.add(event(ACTION_ON, note, pressure));
        activeNote = note;
        return List.copyOf(events);
    }

    private int noteFor(float pX, float pY) {
        int semitone = Math.min((int) (normalize(pX) * NOTES_PER_OCTAVE), NOTES_PER_OCTAVE - 1);
        int octave = Math.min((int) (normalize(pY) * HUION_OCTAVE_COUNT), HUION_OCTAVE_COUNT - 1);
        return Math.clamp(HUION_BASE_NOTE + octave * NOTES_PER_OCTAVE + semitone, 0, 127);
    }

    private void releaseActiveNote(List<SynthEvent> pEvents) {
        if (activeNote != NO_ACTIVE_NOTE) {
            pEvents.add(event(ACTION_OFF, activeNote, 0f));
            activeNote = NO_ACTIVE_NOTE;
        }
    }

    private SynthEvent event(int pAction, int pNote, float pPressure) {
        return new SynthEvent(0L, SYNTH_SAXOPHONE_ID, pAction, pNote, 0,
                pPressure, 0f, 0f, 0f);
    }

    private float normalize(float pValue) {
        if (!Float.isFinite(pValue)) {
            return 0f;
        }
        return Math.clamp(pValue, 0f, 1f);
    }
}
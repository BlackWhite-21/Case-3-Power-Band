package powerband.instrument;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static powerband.util.IConstants.ACTION_OFF;
import static powerband.util.IConstants.ACTION_ON;
import static powerband.util.IConstants.SYNTH_SAXOPHONE_ID;

import java.util.List;

import org.junit.jupiter.api.Test;

import powerband.dto.SynthEvent;
import powerband.dto.TabletReading;

class HuionInputMapperTest {

    @Test
    void mapsTabletCornersToConfiguredMidiRange() {
        HuionInputMapper mapper = new HuionInputMapper();

        SynthEvent lowest = mapper.map(new TabletReading(0f, 0f, 0.5f)).getFirst();
        assertEquals(48, lowest.note());
        assertEquals(SYNTH_SAXOPHONE_ID, lowest.synthId());

        SynthEvent highest = new HuionInputMapper()
                .map(new TabletReading(1f, 1f, 0.5f)).getFirst();
        assertEquals(71, highest.note());
    }

    @Test
    void clampsCoordinatesAndPressureToTabletRange() {
        SynthEvent event = new HuionInputMapper()
                .map(new TabletReading(2f, -1f, 2f)).getFirst();

        assertEquals(59, event.note());
        assertEquals(1f, event.velocity());
    }

    @Test
    void emitsOffThenOnWhenPenMovesToAnotherNote() {
        HuionInputMapper mapper = new HuionInputMapper();
        mapper.map(new TabletReading(0f, 0f, 0.4f));

        List<SynthEvent> events = mapper.map(new TabletReading(0.5f, 0f, 0.8f));

        assertEquals(2, events.size());
        assertEquals(ACTION_OFF, events.getFirst().action());
        assertEquals(48, events.getFirst().note());
        assertEquals(ACTION_ON, events.getLast().action());
        assertEquals(54, events.getLast().note());
        assertEquals(0.8f, events.getLast().velocity());
    }

    @Test
    void liftingPenReleasesOnlyTheCurrentlyActiveNote() {
        HuionInputMapper mapper = new HuionInputMapper();
        mapper.map(new TabletReading(0.25f, 0f, 0.6f));

        List<SynthEvent> release = mapper.map(new TabletReading(0.25f, 0f, 0f));

        assertEquals(1, release.size());
        assertEquals(ACTION_OFF, release.getFirst().action());
        assertEquals(51, release.getFirst().note());
        assertTrue(mapper.map(new TabletReading(0.25f, 0f, 0f)).isEmpty());
    }
}
package powerband.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static powerband.util.IConstants.ACTION_ON;
import static powerband.util.IConstants.BTN_PAUSE_RECORDING;
import static powerband.util.IConstants.BTN_RECORD;
import static powerband.util.IConstants.BTN_STOP_PLAY;
import static powerband.util.IConstants.COL_ACTION;
import static powerband.util.IConstants.COL_DURATION;
import static powerband.util.IConstants.COL_NOTE;
import static powerband.util.IConstants.COL_TIME;
import static powerband.util.IConstants.COL_VOLUME;
import static powerband.util.IConstants.COL_TIMBRE_1;
import static powerband.util.IConstants.COL_TIMBRE_2;
import static powerband.util.IConstants.COL_TIMBRE_3;
import static powerband.util.IConstants.COL_SYNTH;
import static powerband.util.IConstants.SYNTH_ORGAN_ID;
import static powerband.util.IConstants.UI_LOGO_COLOR;
import static powerband.util.IConstants.LOGO_TEXT;
import static powerband.util.IConstants.TAB_EDITOR;
import static powerband.util.IConstants.TAB_PLAYER;
import static powerband.util.IConstants.TAB_RECORDER;
import static powerband.util.IConstants.TIMELINE_COLUMN_COUNT;
import static powerband.util.IConstants.TIMELINE_NOTE_COLUMN;
import static powerband.util.IConstants.TIMELINE_TIME_COLUMN;
import static powerband.util.IConstants.TIMELINE_DURATION_COLUMN;
import static powerband.util.IConstants.TIMELINE_VOLUME_COLUMN;
import static powerband.util.IConstants.TIMELINE_TIMBRE_1_COLUMN;
import static powerband.util.IConstants.TIMELINE_TIMBRE_2_COLUMN;
import static powerband.util.IConstants.TIMELINE_TIMBRE_3_COLUMN;

import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import powerband.dto.SynthEvent;
import powerband.repository.BinaryRecordingRepository;
import powerband.service.RecordingService;
import powerband.util.IConstants;

class RecordingEditorPanelTest {

    @TempDir
    private java.nio.file.Path temporaryDirectory;

    @Test
    void showsTheThreeEditorPagesAndPowerBandBranding() throws Exception {
        RecordingEditorPanel[] pPanel = new RecordingEditorPanel[1];
        SwingUtilities.invokeAndWait(() -> pPanel[0] = createPanel());
        List<Component> pComponents = componentsOf(pPanel[0]);
        List<String> pButtonLabels = pComponents.stream()
                .filter(JButton.class::isInstance)
                .map(JButton.class::cast)
                .map(JButton::getText)
                .toList();
        JLabel pLogo = pComponents.stream()
                .filter(JLabel.class::isInstance)
                .map(JLabel.class::cast)
                .filter(pLabel -> LOGO_TEXT.equals(pLabel.getText()))
                .findFirst()
                .orElseThrow();

        assertTrue(pButtonLabels.contains(TAB_EDITOR));
        assertTrue(pButtonLabels.contains(TAB_RECORDER));
        assertTrue(pButtonLabels.contains(TAB_PLAYER));
        assertEquals(UI_LOGO_COLOR, pLogo.getForeground());
    }

    @Test
    void timelineEditsChangeOnlyTheWorkingCopy() throws Exception {
        RecordingEditorPanel[] pPanel = new RecordingEditorPanel[1];
        SynthEvent pOriginalEvent = new SynthEvent(
                100, SYNTH_ORGAN_ID, ACTION_ON, 60, 0, 0.8f, 0.2f, 0.3f, 0.4f);
        SwingUtilities.invokeAndWait(() -> {
            pPanel[0] = createPanel();
            pPanel[0].setLoadedRecording("original", List.of(pOriginalEvent));
        });
        JTable pTimeline = findTable(pPanel[0]);

        SwingUtilities.invokeAndWait(() -> {
            pTimeline.getModel().setValueAt("250", 0, TIMELINE_TIME_COLUMN);
            pTimeline.getModel().setValueAt("65", 0, TIMELINE_NOTE_COLUMN);
            pTimeline.getModel().setValueAt("500", 0, TIMELINE_DURATION_COLUMN);
            pTimeline.getModel().setValueAt("0.6", 0, TIMELINE_VOLUME_COLUMN);
            pTimeline.getModel().setValueAt("0.1", 0, TIMELINE_TIMBRE_1_COLUMN);
            pTimeline.getModel().setValueAt("0.5", 0, TIMELINE_TIMBRE_2_COLUMN);
            pTimeline.getModel().setValueAt("0.9", 0, TIMELINE_TIMBRE_3_COLUMN);
        });

        assertEquals(TIMELINE_COLUMN_COUNT, pTimeline.getColumnCount());
        assertEquals("250", pTimeline.getValueAt(0, TIMELINE_TIME_COLUMN));
        assertEquals("65", pTimeline.getValueAt(0, TIMELINE_NOTE_COLUMN));
        assertEquals("500", pTimeline.getValueAt(0, TIMELINE_DURATION_COLUMN));
        assertEquals("0.6", pTimeline.getValueAt(0, TIMELINE_VOLUME_COLUMN));
        assertEquals("0.1", pTimeline.getValueAt(0, TIMELINE_TIMBRE_1_COLUMN));
        assertEquals("0.5", pTimeline.getValueAt(0, TIMELINE_TIMBRE_2_COLUMN));
        assertEquals("0.9", pTimeline.getValueAt(0, TIMELINE_TIMBRE_3_COLUMN));
        assertEquals(60, pOriginalEvent.note());
        assertEquals(100L, pOriginalEvent.offsetMs());
        assertFalse(pTimeline.getModel().isCellEditable(0, IConstants.TIMELINE_SYNTH_COLUMN));
        assertFalse(pTimeline.getModel().isCellEditable(0, IConstants.TIMELINE_ACTION_COLUMN));
        assertEquals(COL_TIME, pTimeline.getColumnName(TIMELINE_TIME_COLUMN));
        assertEquals(COL_SYNTH, pTimeline.getColumnName(IConstants.TIMELINE_SYNTH_COLUMN));
        assertEquals(COL_ACTION, pTimeline.getColumnName(IConstants.TIMELINE_ACTION_COLUMN));
        assertEquals(COL_NOTE, pTimeline.getColumnName(TIMELINE_NOTE_COLUMN));
        assertEquals(COL_DURATION, pTimeline.getColumnName(TIMELINE_DURATION_COLUMN));
        assertEquals(COL_VOLUME, pTimeline.getColumnName(TIMELINE_VOLUME_COLUMN));
        assertEquals(COL_TIMBRE_1, pTimeline.getColumnName(TIMELINE_TIMBRE_1_COLUMN));
        assertEquals(COL_TIMBRE_2, pTimeline.getColumnName(TIMELINE_TIMBRE_2_COLUMN));
        assertEquals(COL_TIMBRE_3, pTimeline.getColumnName(TIMELINE_TIMBRE_3_COLUMN));
    }

    @Test
    void recordingControlsAreExclusiveToRecorderPage() throws Exception {
        RecordingEditorPanel[] pPanel = new RecordingEditorPanel[1];
        SwingUtilities.invokeAndWait(() -> pPanel[0] = createPanel());
        List<Component> pComponents = componentsOf(pPanel[0]);

        assertEquals(List.of(BTN_RECORD, BTN_PAUSE_RECORDING),
                buttonsOnPage(pComponents, IConstants.PAGE_RECORDER));
        assertEquals(IConstants.PAGE_PLAYER, pageOfButton(pComponents, BTN_STOP_PLAY));
    }

    @Test
    void playerPageContainsExactNameSearchControl() throws Exception {
        RecordingEditorPanel[] pPanel = new RecordingEditorPanel[1];
        SwingUtilities.invokeAndWait(() -> pPanel[0] = createPanel());
        List<Component> pComponents = componentsOf(pPanel[0]);
        List<JTextField> pFields = pComponents.stream()
                .filter(JTextField.class::isInstance)
                .map(JTextField.class::cast)
                .toList();
        List<String> pButtonLabels = pComponents.stream()
                .filter(JButton.class::isInstance)
                .map(JButton.class::cast)
                .map(JButton::getText)
                .toList();

        assertEquals(1, pFields.size());
        assertEquals(IConstants.EMPTY_STRING, pFields.getFirst().getText());
        assertTrue(pButtonLabels.contains(IConstants.BTN_SEARCH_RECORDING));
    }

    private RecordingEditorPanel createPanel() {
        return new RecordingEditorPanel(new RecordingService(
                new BinaryRecordingRepository(temporaryDirectory)));
    }

    private List<Component> componentsOf(Container pContainer) {
        List<Component> pComponents = new ArrayList<>();
        collectComponents(pContainer, pComponents);
        return pComponents;
    }

    private void collectComponents(Container pContainer, List<Component> pComponents) {
        for (Component pComponent : pContainer.getComponents()) {
            pComponents.add(pComponent);
            if (pComponent instanceof Container pChild) {
                collectComponents(pChild, pComponents);
            }
        }
    }

    private String pageOfButton(List<Component> pComponents, String pButtonLabel) {
        JButton pButton = pComponents.stream()
                .filter(JButton.class::isInstance)
                .map(JButton.class::cast)
                .filter(pCandidate -> pButtonLabel.equals(pCandidate.getText()))
                .findFirst()
                .orElseThrow();
        return pageOfComponent(pButton);
    }

    private String pageOfComponent(Component pComponent) {
        Container pParent = pComponent.getParent();
        while (pParent != null) {
            if (pParent instanceof JPanel pPanel
                    && (IConstants.PAGE_EDITOR.equals(pPanel.getName())
                    || IConstants.PAGE_RECORDER.equals(pPanel.getName())
                    || IConstants.PAGE_PLAYER.equals(pPanel.getName()))) {
                return pPanel.getName();
            }
            pParent = pParent.getParent();
        }
        return null;
    }

    private List<String> buttonsOnPage(List<Component> pComponents, String pPageName) {
        return pComponents.stream()
                .filter(JButton.class::isInstance)
                .map(JButton.class::cast)
                .filter(pButton -> pPageName.equals(pageOfComponent(pButton)))
                .map(JButton::getText)
                .toList();
    }

    private JTable findTable(Container pContainer) {
        return componentsOf(pContainer).stream()
                .filter(JTable.class::isInstance)
                .map(JTable.class::cast)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Timeline table was not found"));
    }
}

package powerband.ui;

import static powerband.util.IConstants.APP_TITLE;
import static powerband.util.IConstants.BTN_RECORD;
import static powerband.util.IConstants.BTN_SAVE_RECORDING;
import static powerband.util.IConstants.BTN_STOP_RECORD;
import static powerband.util.IConstants.BTN_LOAD;
import static powerband.util.IConstants.BTN_PLAY;
import static powerband.util.IConstants.BTN_STOP_PLAY;
import static powerband.util.IConstants.IDLE_COLOR;
import static powerband.util.IConstants.LBL_RECORDING;
import static powerband.util.IConstants.LBL_WAITING_FOR_NAME;
import static powerband.util.IConstants.LBL_LOADED;
import static powerband.util.IConstants.LBL_PLAYING;
import static powerband.util.IConstants.MSG_ASK_NAME;
import static powerband.util.IConstants.MSG_ERROR_TITLE;
import static powerband.util.IConstants.MSG_SAVED_RECORDING;
import static powerband.util.IConstants.MSG_SELECT;
import static powerband.util.IConstants.MSG_EMPTY_LIST;
import static powerband.util.IConstants.RECORDING_COLOR;
import static powerband.util.IConstants.ERR_UNKNOWN_RECORDING;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.List;
import java.util.Objects;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import powerband.dto.RecordingInfo;
import powerband.exception.DuplicateNameException;
import powerband.exception.InvalidNameException;
import powerband.exception.RecordingException;
import powerband.exception.PowerBandException;
import powerband.exception.RecordingNotFoundException;
import powerband.dto.LoadedRecording;
import powerband.service.RecordingService;
import powerband.util.IConstants;

public final class RecordingEditorPanel extends JPanel {

    private final RecordingService recordingService;
    private final JButton recordingButton;
    private final JButton loadButton;
    private final JButton playButton;
    private final JButton stopPlayButton;
    private final JLabel statusLabel;
    private PanelState panelState;

    public RecordingEditorPanel(RecordingService pRecordingService) {
        super(new BorderLayout(IConstants.UI_COMPONENT_GAP, IConstants.UI_COMPONENT_GAP));
        this.recordingService = Objects.requireNonNull(pRecordingService);
        this.recordingButton = new JButton(BTN_RECORD);
        this.loadButton = new JButton(BTN_LOAD);
        this.playButton = new JButton(BTN_PLAY);
        this.stopPlayButton = new JButton(BTN_STOP_PLAY);
        this.statusLabel = new JLabel(IConstants.LBL_NO_RECORDING, SwingConstants.CENTER);
        this.panelState = PanelState.READY;
        initializeComponents();
    }

    private void initializeComponents() {
        JPanel pControls = new JPanel(new FlowLayout(FlowLayout.CENTER));
        pControls.add(recordingButton);
        pControls.add(loadButton);
        pControls.add(playButton);
        pControls.add(stopPlayButton);
        add(pControls, BorderLayout.CENTER);
        add(statusLabel, BorderLayout.SOUTH);
        recordingButton.addActionListener(pEvent -> handleRecordingButton());
        loadButton.addActionListener(pEvent -> loadRecording());
        playButton.addActionListener(pEvent -> playRecording());
        stopPlayButton.addActionListener(pEvent -> stopPlayback());
        playButton.setEnabled(false);
        stopPlayButton.setEnabled(false);
        recordingService.setPlaybackListener(pException ->
                showPlaybackError(pException));
        recordingService.setPlaybackCompletionListener(this::showPlaybackCompleted);
    }

    private void handleRecordingButton() {
        switch (panelState) {
            case READY -> startRecording();
            case RECORDING -> stopRecording();
            case WAITING_FOR_NAME -> requestRecordingName();
            case LOADED -> {
            }
            case PLAYING -> stopPlayback();
        }
    }

    private void startRecording() {
        try {
            recordingService.startRecording();
            panelState = PanelState.RECORDING;
            recordingButton.setText(BTN_STOP_RECORD);
            statusLabel.setText(LBL_RECORDING);
            statusLabel.setForeground(RECORDING_COLOR);
            loadButton.setEnabled(false);
            playButton.setEnabled(false);
            stopPlayButton.setEnabled(false);
        } catch (RecordingException pException) {
            showError(pException);
        }
    }

    private void stopRecording() {
        try {
            recordingService.stopRecording();
            panelState = PanelState.WAITING_FOR_NAME;
            recordingButton.setText(BTN_SAVE_RECORDING);
            statusLabel.setText(LBL_WAITING_FOR_NAME);
            statusLabel.setForeground(IDLE_COLOR);
            requestRecordingName();
        } catch (RecordingException pException) {
            showError(pException);
        }
    }

    private void requestRecordingName() {
        while (recordingService.hasPendingName()) {
            String pName = JOptionPane.showInputDialog(this, MSG_ASK_NAME, APP_TITLE, JOptionPane.QUESTION_MESSAGE);
            if (pName == null) {
                statusLabel.setText(LBL_WAITING_FOR_NAME);
                return;
            }
            try {
                RecordingInfo pRecordingInfo = recordingService.saveRecording(pName);
                panelState = PanelState.READY;
                recordingButton.setText(BTN_RECORD);
                statusLabel.setText(String.format(MSG_SAVED_RECORDING, pRecordingInfo.name()));
                loadButton.setEnabled(true);
                return;
            } catch (InvalidNameException | DuplicateNameException pException) {
                showError(pException);
            } catch (RecordingException pException) {
                showError(pException);
                return;
            }
        }
    }

    private void loadRecording() {
        try {
            List<RecordingInfo> pRecordings = recordingService.listRecordings();
            if (pRecordings.isEmpty()) {
                JOptionPane.showMessageDialog(
                        this,
                        MSG_EMPTY_LIST,
                        MSG_ERROR_TITLE,
                        JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            Object pSelected = JOptionPane.showInputDialog(
                    this,
                    MSG_SELECT,
                    APP_TITLE,
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    pRecordings.toArray(),
                    pRecordings.get(0));
            if (pSelected == null) {
                return;
            }
            RecordingInfo pSelectedRecording = (RecordingInfo) pSelected;
            LoadedRecording pLoaded = recordingService.loadRecording(pSelectedRecording.name());
            panelState = PanelState.LOADED;
            statusLabel.setText(String.format(
                    LBL_LOADED,
                    pLoaded.name(),
                    pLoaded.headerPosition(),
                    pLoaded.endOffset(),
                    (pLoaded.endOffset()
                            - pLoaded.headerPosition()
                            - IConstants.MUSIC_RECORD_SIZE)
                            / IConstants.MUSIC_RECORD_SIZE));
            statusLabel.setForeground(IDLE_COLOR);
            playButton.setEnabled(true);
        } catch (RecordingNotFoundException pException) {
            showError(pException);
        } catch (RecordingException pException) {
            showError(pException);
        }
    }

    private void playRecording() {
        try {
            recordingService.playLoadedRecording();
            panelState = PanelState.PLAYING;
            LoadedRecording pLoaded = recordingService.getLoadedRecording();
            statusLabel.setText(String.format(LBL_PLAYING, pLoaded.name()));
            statusLabel.setForeground(RECORDING_COLOR);
            recordingButton.setEnabled(false);
            loadButton.setEnabled(false);
            playButton.setEnabled(false);
            stopPlayButton.setEnabled(true);
        } catch (RecordingException pException) {
            showError(pException);
        }
    }

    private void stopPlayback() {
        try {
            recordingService.stopPlayback();
            panelState = PanelState.LOADED;
            LoadedRecording pLoaded = recordingService.getLoadedRecording();
            statusLabel.setText(pLoaded == null
                    ? IConstants.LBL_NO_RECORDING
                    : String.format(
                            LBL_LOADED,
                            pLoaded.name(),
                            pLoaded.headerPosition(),
                            pLoaded.endOffset(),
                            (pLoaded.endOffset()
                                    - pLoaded.headerPosition()
                                    - IConstants.MUSIC_RECORD_SIZE)
                                    / IConstants.MUSIC_RECORD_SIZE));
            recordingButton.setEnabled(true);
            loadButton.setEnabled(true);
            playButton.setEnabled(true);
            stopPlayButton.setEnabled(false);
        } catch (RecordingException pException) {
            showError(pException);
        }
    }

    private void showPlaybackError(PowerBandException pException) {
        javax.swing.SwingUtilities.invokeLater(() -> {
            panelState = PanelState.LOADED;
            recordingButton.setEnabled(true);
            loadButton.setEnabled(true);
            playButton.setEnabled(true);
            stopPlayButton.setEnabled(false);
            showError(pException);
        });
    }

    private void showPlaybackCompleted() {
        javax.swing.SwingUtilities.invokeLater(() -> {
            if (panelState != PanelState.PLAYING) {
                return;
            }
            panelState = PanelState.LOADED;
            LoadedRecording pLoaded = recordingService.getLoadedRecording();
            statusLabel.setText(pLoaded == null
                    ? IConstants.LBL_NO_RECORDING
                    : String.format(
                            LBL_LOADED,
                            pLoaded.name(),
                            pLoaded.headerPosition(),
                            pLoaded.endOffset(),
                            (pLoaded.endOffset()
                                    - pLoaded.headerPosition()
                                    - IConstants.MUSIC_RECORD_SIZE)
                                    / IConstants.MUSIC_RECORD_SIZE));
            statusLabel.setForeground(IDLE_COLOR);
            recordingButton.setEnabled(true);
            loadButton.setEnabled(true);
            playButton.setEnabled(true);
            stopPlayButton.setEnabled(false);
        });
    }

    private void showError(RecordingException pException) {
        String pMessage = pException.getMessage();
        if (pMessage == null || pMessage.isBlank()) {
            pMessage = ERR_UNKNOWN_RECORDING;
        }
        JOptionPane.showMessageDialog(this, pMessage, MSG_ERROR_TITLE, JOptionPane.ERROR_MESSAGE);
    }

    private void showError(PowerBandException pException) {
        String pMessage = pException.getMessage();
        if (pMessage == null || pMessage.isBlank()) {
            pMessage = ERR_UNKNOWN_RECORDING;
        }
        JOptionPane.showMessageDialog(this, pMessage, MSG_ERROR_TITLE, JOptionPane.ERROR_MESSAGE);
    }

    private enum PanelState {
        READY,
        RECORDING,
        WAITING_FOR_NAME,
        LOADED,
        PLAYING
    }
}

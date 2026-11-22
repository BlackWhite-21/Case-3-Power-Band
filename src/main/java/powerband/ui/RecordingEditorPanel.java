package powerband.ui;

import static powerband.util.IConstants.APP_TITLE;
import static powerband.util.IConstants.BTN_RECORD;
import static powerband.util.IConstants.BTN_SAVE_RECORDING;
import static powerband.util.IConstants.BTN_STOP_RECORD;
import static powerband.util.IConstants.IDLE_COLOR;
import static powerband.util.IConstants.LBL_RECORDING;
import static powerband.util.IConstants.LBL_WAITING_FOR_NAME;
import static powerband.util.IConstants.MSG_ASK_NAME;
import static powerband.util.IConstants.MSG_ERROR_TITLE;
import static powerband.util.IConstants.MSG_SAVED_RECORDING;
import static powerband.util.IConstants.RECORDING_COLOR;
import static powerband.util.IConstants.ERR_UNKNOWN_RECORDING;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
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
import powerband.service.RecordingService;
import powerband.util.IConstants;

public final class RecordingEditorPanel extends JPanel {

    private final RecordingService recordingService;
    private final JButton recordingButton;
    private final JLabel statusLabel;
    private PanelState panelState;

    public RecordingEditorPanel(RecordingService pRecordingService) {
        super(new BorderLayout(IConstants.UI_COMPONENT_GAP, IConstants.UI_COMPONENT_GAP));
        this.recordingService = Objects.requireNonNull(pRecordingService);
        this.recordingButton = new JButton(BTN_RECORD);
        this.statusLabel = new JLabel(IConstants.LBL_NO_RECORDING, SwingConstants.CENTER);
        this.panelState = PanelState.READY;
        initializeComponents();
    }

    private void initializeComponents() {
        JPanel pControls = new JPanel(new FlowLayout(FlowLayout.CENTER));
        pControls.add(recordingButton);
        add(pControls, BorderLayout.CENTER);
        add(statusLabel, BorderLayout.SOUTH);
        recordingButton.addActionListener(pEvent -> handleRecordingButton());
    }

    private void handleRecordingButton() {
        switch (panelState) {
            case READY -> startRecording();
            case RECORDING -> stopRecording();
            case WAITING_FOR_NAME -> requestRecordingName();
        }
    }

    private void startRecording() {
        try {
            recordingService.startRecording();
            panelState = PanelState.RECORDING;
            recordingButton.setText(BTN_STOP_RECORD);
            statusLabel.setText(LBL_RECORDING);
            statusLabel.setForeground(RECORDING_COLOR);
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
                return;
            } catch (InvalidNameException | DuplicateNameException pException) {
                showError(pException);
            } catch (RecordingException pException) {
                showError(pException);
                return;
            }
        }
    }

    private void showError(RecordingException pException) {
        String pMessage = pException.getMessage();
        if (pMessage == null || pMessage.isBlank()) {
            pMessage = ERR_UNKNOWN_RECORDING;
        }
        JOptionPane.showMessageDialog(this, pMessage, MSG_ERROR_TITLE, JOptionPane.ERROR_MESSAGE);
    }

    private enum PanelState {
        READY,
        RECORDING,
        WAITING_FOR_NAME
    }
}

package powerband.ui;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.table.AbstractTableModel;

import powerband.dto.RecordingInfo;
import powerband.dto.SynthEvent;
import powerband.exception.DuplicateNameException;
import powerband.exception.InvalidNameException;
import powerband.exception.RecordingException;
import powerband.service.RecordingService;
import powerband.util.IConstants;

public final class RecordingEditorPanel extends JPanel {

    private final RecordingService recordingService;
    private final CardLayout pageLayout;
    private final JPanel pages;
    private final TimelineTableModel timelineModel;
    private final JTable timelineTable;
    private final JLabel statusLabel;
    private final JLabel recordingStatusLabel;
    private final JLabel joystickStatusLabel;
    private final JList<RecordingInfo> recordingList;
    private final javax.swing.DefaultListModel<RecordingInfo> recordingListModel;
    private final JButton recorderControlButton;
    private final JButton pauseRecordingButton;
    private final JButton stopPlaybackButton;
    private final JButton saveCopyButton;
    private final JTextField searchNameField;
    private String loadedName;
    private boolean busy;

    public RecordingEditorPanel(RecordingService pRecordingService) {
        super(new BorderLayout(IConstants.UI_COMPONENT_GAP, IConstants.UI_COMPONENT_GAP));
        recordingService = Objects.requireNonNull(pRecordingService);
        pageLayout = new CardLayout();
        pages = new JPanel(pageLayout);
        timelineModel = new TimelineTableModel();
        timelineTable = new JTable(timelineModel);
        statusLabel = new JLabel(IConstants.LBL_READY, SwingConstants.CENTER);
        recordingStatusLabel = new JLabel(IConstants.LBL_NO_RECORDING, SwingConstants.CENTER);
        joystickStatusLabel = new JLabel(
                String.format(IConstants.JOY_STATUS_FORMAT, IConstants.STATUS_STOPPED),
                SwingConstants.CENTER);
        recordingListModel = new javax.swing.DefaultListModel<>();
        recordingList = new JList<>(recordingListModel);
        recorderControlButton = new JButton(IConstants.BTN_RECORD);
        pauseRecordingButton = new JButton(IConstants.BTN_PAUSE_RECORDING);
        stopPlaybackButton = new JButton(IConstants.BTN_STOP_PLAY);
        saveCopyButton = new JButton(IConstants.BTN_SAVE_COPY);
        searchNameField = new JTextField(IConstants.EMPTY_STRING);
        loadedName = IConstants.EMPTY_STRING;
        initializeComponents();
    }

    private void initializeComponents() {
        setBackground(IConstants.UI_BACKGROUND_COLOR);
        add(createHeader(), BorderLayout.NORTH);
        pages.add(createEditorPage(), IConstants.PAGE_EDITOR);
        pages.add(createRecorderPage(), IConstants.PAGE_RECORDER);
        pages.add(createPlayerPage(), IConstants.PAGE_PLAYER);
        pages.setBackground(IConstants.UI_BACKGROUND_COLOR);
        add(pages, BorderLayout.CENTER);
        add(statusLabel, BorderLayout.SOUTH);
        styleLabel(statusLabel);
        styleLabel(recordingStatusLabel);
        styleLabel(joystickStatusLabel);
        styleTable();
        recorderControlButton.addActionListener(pEvent -> toggleRecording());
        pauseRecordingButton.addActionListener(pEvent -> togglePauseRecording());
        stopPlaybackButton.addActionListener(pEvent -> stopPlayback());
        saveCopyButton.addActionListener(pEvent -> promptCopyName());
        setRecordingControls();
    }

    private JPanel createHeader() {
        JPanel pHeader = new JPanel(new BorderLayout());
        pHeader.setBackground(IConstants.UI_BACKGROUND_COLOR);
        JLabel pLogo = new JLabel(IConstants.LOGO_TEXT, SwingConstants.CENTER);
        pLogo.setForeground(IConstants.UI_LOGO_COLOR);
        pLogo.setFont(new Font(
                IConstants.UI_FONT_FAMILY,
                Font.BOLD,
                IConstants.UI_LOGO_FONT_SIZE));
        pHeader.add(pLogo, BorderLayout.NORTH);

        JPanel pNavigation = new JPanel(new FlowLayout(FlowLayout.CENTER, IConstants.UI_COMPONENT_GAP, IConstants.ZERO_INT));
        pNavigation.setBackground(IConstants.UI_BACKGROUND_COLOR);
        pNavigation.add(createNavigationButton(IConstants.TAB_EDITOR, IConstants.PAGE_EDITOR));
        pNavigation.add(createNavigationButton(IConstants.TAB_RECORDER, IConstants.PAGE_RECORDER));
        pNavigation.add(createNavigationButton(IConstants.TAB_PLAYER, IConstants.PAGE_PLAYER));
        pHeader.add(pNavigation, BorderLayout.CENTER);
        pHeader.add(joystickStatusLabel, BorderLayout.SOUTH);

        return pHeader;
    }

    public void setJoystickStatus(String pStatus) {
        SwingUtilities.invokeLater(() -> joystickStatusLabel.setText(
                String.format(IConstants.JOY_STATUS_FORMAT, pStatus)));
    }

    private JButton createNavigationButton(String pLabel, String pPage) {
        JButton pButton = new JButton(pLabel);
        styleButton(pButton);
        pButton.addActionListener(pEvent -> showPage(pPage));
        return pButton;
    }

    private JPanel createEditorPage() {
        JPanel pPage = createPagePanel();
        pPage.setName(IConstants.PAGE_EDITOR);
        JPanel pTools = new JPanel(new BorderLayout(IConstants.UI_COMPONENT_GAP, IConstants.UI_COMPONENT_GAP));
        pTools.setBackground(IConstants.UI_PANEL_COLOR);
        JLabel pDescription = new JLabel(IConstants.MSG_EDITED_RECORDING);
        pDescription.setForeground(IConstants.UI_TEXT_COLOR);
        pTools.add(pDescription, BorderLayout.CENTER);
        saveCopyButton.setEnabled(false);
        styleButton(saveCopyButton);
        pTools.add(saveCopyButton, BorderLayout.EAST);
        pPage.add(pTools, BorderLayout.NORTH);
        JScrollPane pTimelineScroll = new JScrollPane(timelineTable);
        pTimelineScroll.getViewport().setBackground(IConstants.UI_PANEL_COLOR);
        pTimelineScroll.setBackground(IConstants.UI_PANEL_COLOR);
        pPage.add(pTimelineScroll, BorderLayout.CENTER);
        return pPage;
    }

    private JPanel createRecorderPage() {
        JPanel pPage = createPagePanel();
        pPage.setName(IConstants.PAGE_RECORDER);
        JLabel pDescription = new JLabel(IConstants.LBL_RECORDING_PAGE, SwingConstants.CENTER);
        pDescription.setForeground(IConstants.UI_TEXT_COLOR);
        JPanel pInformation = new JPanel(new BorderLayout(IConstants.UI_COMPONENT_GAP, IConstants.UI_COMPONENT_GAP));
        pInformation.setBackground(IConstants.UI_PANEL_COLOR);
        pInformation.add(pDescription, BorderLayout.NORTH);
        pInformation.add(recordingStatusLabel, BorderLayout.CENTER);
        JPanel pControls = new JPanel(new FlowLayout(FlowLayout.CENTER, IConstants.UI_COMPONENT_GAP, IConstants.ZERO_INT));
        pControls.setBackground(IConstants.UI_PANEL_COLOR);
        styleButton(recorderControlButton);
        styleButton(pauseRecordingButton);
        pControls.add(recorderControlButton);
        pControls.add(pauseRecordingButton);
        pPage.add(pInformation, BorderLayout.CENTER);
        pPage.add(pControls, BorderLayout.SOUTH);
        return pPage;
    }

    private JPanel createPlayerPage() {
        JPanel pPage = createPagePanel();
        pPage.setName(IConstants.PAGE_PLAYER);
        JPanel pTop = new JPanel(new BorderLayout());
        pTop.setBackground(IConstants.UI_PANEL_COLOR);
        JLabel pDescription = new JLabel(IConstants.LBL_PLAYER_PAGE);
        pDescription.setForeground(IConstants.UI_TEXT_COLOR);
        pTop.add(pDescription, BorderLayout.CENTER);
        JButton pRefreshButton = new JButton(IConstants.BTN_LOAD);
        styleButton(pRefreshButton);
        pRefreshButton.addActionListener(pEvent -> refreshRecordingList());
        pTop.add(pRefreshButton, BorderLayout.EAST);
        JPanel pSearch = new JPanel(new FlowLayout(
                FlowLayout.CENTER,
                IConstants.UI_COMPONENT_GAP,
                IConstants.ZERO_INT));
        pSearch.setBackground(IConstants.UI_PANEL_COLOR);
        JLabel pSearchLabel = new JLabel(IConstants.FLD_SEARCH_RECORDING);
        pSearchLabel.setForeground(IConstants.UI_TEXT_COLOR);
        searchNameField.setColumns(IConstants.NAME_LENGTH);
        JButton pSearchButton = new JButton(IConstants.BTN_SEARCH_RECORDING);
        styleButton(pSearchButton);
        pSearchButton.addActionListener(pEvent -> searchRecording());
        pSearch.add(pSearchLabel);
        pSearch.add(searchNameField);
        pSearch.add(pSearchButton);
        pTop.add(pSearch, BorderLayout.SOUTH);
        pPage.add(pTop, BorderLayout.NORTH);

        recordingList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        recordingList.setBackground(IConstants.UI_PANEL_COLOR);
        recordingList.setForeground(IConstants.UI_TEXT_COLOR);
        recordingList.setSelectionBackground(IConstants.UI_ACCENT_COLOR);
        recordingList.setSelectionForeground(IConstants.UI_TEXT_COLOR);
        JScrollPane pRecordingScroll = new JScrollPane(recordingList);
        pRecordingScroll.getViewport().setBackground(IConstants.UI_PANEL_COLOR);
        pRecordingScroll.setBackground(IConstants.UI_PANEL_COLOR);
        pPage.add(pRecordingScroll, BorderLayout.CENTER);
        JPanel pControls = new JPanel(new FlowLayout(FlowLayout.CENTER, IConstants.UI_COMPONENT_GAP, IConstants.ZERO_INT));
        pControls.setBackground(IConstants.UI_PANEL_COLOR);
        JButton pLoadButton = new JButton(IConstants.BTN_LOAD_SELECTED);
        JButton pPlayButton = new JButton(IConstants.BTN_PLAY_SELECTED);
        styleButton(pLoadButton);
        styleButton(pPlayButton);
        styleButton(stopPlaybackButton);
        pLoadButton.addActionListener(pEvent -> loadSelectedRecording());
        pPlayButton.addActionListener(pEvent -> playSelectedRecording());
        pControls.add(pLoadButton);
        pControls.add(pPlayButton);
        pControls.add(stopPlaybackButton);
        pPage.add(pControls, BorderLayout.SOUTH);
        return pPage;
    }

    private JPanel createPagePanel() {
        JPanel pPanel = new JPanel(new BorderLayout(IConstants.UI_COMPONENT_GAP, IConstants.UI_COMPONENT_GAP));
        pPanel.setBackground(IConstants.UI_PANEL_COLOR);
        return pPanel;
    }

    private void styleButton(JButton pButton) {
        pButton.setBackground(IConstants.UI_ACCENT_COLOR);
        pButton.setForeground(IConstants.UI_TEXT_COLOR);
        pButton.setFocusPainted(false);
    }

    private void styleLabel(JLabel pLabel) {
        pLabel.setBackground(IConstants.UI_BACKGROUND_COLOR);
        pLabel.setForeground(IConstants.UI_TEXT_COLOR);
        pLabel.setOpaque(true);
    }

    private void styleTable() {
        timelineTable.setBackground(IConstants.UI_PANEL_COLOR);
        timelineTable.setForeground(IConstants.UI_TEXT_COLOR);
        timelineTable.setGridColor(IConstants.UI_TABLE_GRID_COLOR);
        timelineTable.setSelectionBackground(IConstants.UI_ACCENT_COLOR);
        timelineTable.setSelectionForeground(IConstants.UI_TEXT_COLOR);
        timelineTable.setFillsViewportHeight(true);
        timelineTable.getTableHeader().setBackground(IConstants.UI_ACCENT_COLOR);
        timelineTable.getTableHeader().setForeground(IConstants.UI_TEXT_COLOR);
    }

    private void showPage(String pPage) {
        pageLayout.show(pages, pPage);
        if (IConstants.PAGE_PLAYER.equals(pPage)) {
            refreshRecordingList();
        }
    }

    private void toggleRecording() {
        if (recordingService.isRecording()) {
            stopRecording();
        } else {
            startRecording();
        }
    }

    private void togglePauseRecording() {
        boolean pWasPaused = recordingService.isPaused();
        executeAsync(() -> {
            if (pWasPaused) {
                recordingService.resumeRecording();
            } else {
                recordingService.pauseRecording();
            }
            return Boolean.TRUE;
        }, pResumed -> {
            recordingStatusLabel.setText(recordingService.isPaused()
                    ? IConstants.LBL_RECORDING_PAUSED
                    : IConstants.LBL_RECORDING);
            statusLabel.setText(recordingStatusLabel.getText());
            setRecordingControls();
        }, this::showFailure);
    }

    private void startRecording() {
        executeAsync(() -> {
            recordingService.startRecording();
            return Boolean.TRUE;
        }, pStarted -> {
            setRecordingControls();
            recordingStatusLabel.setText(IConstants.LBL_RECORDING);
            statusLabel.setText(IConstants.LBL_RECORDING);
        }, this::showFailure);
    }

    private void stopRecording() {
        executeAsync(() -> {
            recordingService.stopRecording();
            return Boolean.TRUE;
        }, pStopped -> {
            setRecordingControls();
            recordingStatusLabel.setText(IConstants.LBL_WAITING_FOR_NAME);
            statusLabel.setText(IConstants.LBL_WAITING_FOR_NAME);
            confirmSaveRecording();
        }, this::showFailure);
    }

    private void setRecordingControls() {
        boolean pRecording = recordingService.isRecording();
        boolean pPaused = recordingService.isPaused();
        boolean pPendingName = recordingService.hasPendingName();
        String pButtonText = pRecording
                ? IConstants.BTN_STOP_RECORD
                : IConstants.BTN_RECORD;
        recorderControlButton.setText(pButtonText);
        recorderControlButton.setEnabled(!busy && (pRecording || !pPendingName));
        pauseRecordingButton.setText(pPaused
                ? IConstants.BTN_RESUME_RECORDING
                : IConstants.BTN_PAUSE_RECORDING);
        pauseRecordingButton.setEnabled(!busy && pRecording);
    }

    private void confirmSaveRecording() {
        Object[] pOptions = {
                IConstants.BTN_YES_SAVE_RECORDING,
                IConstants.BTN_NO_DISCARD_RECORDING
        };
        int pChoice = JOptionPane.showOptionDialog(
                this,
                IConstants.MSG_CONFIRM_SAVE_RECORDING,
                IConstants.APP_TITLE,
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                pOptions,
                pOptions[IConstants.ZERO_INT]);
        if (pChoice == IConstants.ZERO_INT) {
            promptRecordingName();
        } else {
            discardPendingRecording();
        }
    }

    private void promptRecordingName() {
        String pName = JOptionPane.showInputDialog(
                this,
                IConstants.MSG_ASK_NAME,
                IConstants.APP_TITLE,
                JOptionPane.QUESTION_MESSAGE);
        if (pName == null) {
            confirmSaveRecording();
            return;
        }
        executeAsync(() -> recordingService.saveRecording(pName), pRecording -> {
            recordingStatusLabel.setText(String.format(IConstants.MSG_SAVED_RECORDING, pRecording.name()));
            statusLabel.setText(String.format(IConstants.MSG_SAVED_RECORDING, pRecording.name()));
            setRecordingControls();
            refreshRecordingList();
        }, pFailure -> {
            showFailure(pFailure);
            if (pFailure instanceof InvalidNameException || pFailure instanceof DuplicateNameException) {
                SwingUtilities.invokeLater(this::promptRecordingName);
            }
        });
    }

    private void discardPendingRecording() {
        executeAsync(() -> {
            recordingService.discardRecording();
            return Boolean.TRUE;
        }, pDiscarded -> {
            recordingStatusLabel.setText(IConstants.LBL_RECORDING_DISCARDED);
            statusLabel.setText(IConstants.LBL_RECORDING_DISCARDED);
            setRecordingControls();
        }, pFailure -> {
            showFailure(pFailure);
            confirmSaveRecording();
        });
    }

    private void loadSelectedRecording() {
        RecordingInfo pSelected = recordingList.getSelectedValue();
        if (pSelected == null) {
            showInformation(IConstants.MSG_SELECT_RECORDING);
            return;
        }
        loadRecording(pSelected.name());
    }

    private void loadRecording(String pName) {
        executeAsync(() -> recordingService.loadRecording(pName), pEvents -> {
            setLoadedRecording(pName, pEvents);
            showPage(IConstants.PAGE_EDITOR);
        }, this::showFailure);
    }

    private void searchRecording() {
        String pName = searchNameField.getText();
        if (pName == null || pName.isBlank()) {
            showFailure(new powerband.exception.RecordingException(IConstants.MSG_SELECT_RECORDING));
            return;
        }
        executeAsync(() -> recordingService.searchRecording(pName.strip()), pEvents -> {
            setLoadedRecording(pName.strip(), pEvents);
            showPage(IConstants.PAGE_EDITOR);
        }, this::showFailure);
    }

    private void refreshRecordingList() {
        executeAsync(recordingService::listRecordings, pRecordings -> {
            recordingListModel.clear();
            for (RecordingInfo pRecording : pRecordings) {
                recordingListModel.addElement(pRecording);
            }
        }, this::showFailure);
    }

    private void playSelectedRecording() {
        RecordingInfo pSelected = recordingList.getSelectedValue();
        if (pSelected == null) {
            showInformation(IConstants.MSG_SELECT_RECORDING);
            return;
        }
        if (pSelected.name().equals(loadedName)) {
            playCurrentRecording();
        } else {
            executeAsync(() -> recordingService.loadRecording(pSelected.name()), pEvents -> {
                setLoadedRecording(pSelected.name(), pEvents);
                playCurrentRecording();
            }, this::showFailure);
        }
    }

    void setLoadedRecording(String pName, List<SynthEvent> pEvents) {
        loadedName = pName;
        timelineModel.replaceEvents(pEvents);
        saveCopyButton.setEnabled(true);
        statusLabel.setText(String.format(IConstants.LBL_LOADED_RECORDING, pName));
        recordingStatusLabel.setText(statusLabel.getText());
    }

    private void playCurrentRecording() {
        List<SynthEvent> pEvents = timelineModel.snapshot();
        if (loadedName.isEmpty()) {
            showInformation(IConstants.ERR_NOTHING_LOADED);
            return;
        }
        try {
            CompletableFuture<Void> pPlayback = !timelineModel.isDirty()
                    && recordingService.getLoadedRecording() != null
                            ? recordingService.playLoadedRecording()
                            : recordingService.playRecording(pEvents);
            statusLabel.setText(String.format(IConstants.LBL_PLAYING, loadedName));
            pPlayback.whenComplete((pResult, pFailure) -> SwingUtilities.invokeLater(() -> {
                if (pFailure == null) {
                    statusLabel.setText(IConstants.LBL_PLAYBACK_COMPLETE);
                } else if (pFailure instanceof CancellationException
                        || pFailure.getCause() instanceof CancellationException) {
                    statusLabel.setText(IConstants.LBL_PLAYBACK_STOPPED);
                } else {
                    showFailure(pFailure.getCause() == null ? pFailure : pFailure.getCause());
                }
            }));
        } catch (RecordingException pException) {
            showFailure(pException);
        }
    }

    private void stopPlayback() {
        recordingService.stopPlayback();
        statusLabel.setText(IConstants.LBL_PLAYBACK_STOPPED);
    }

    private void promptCopyName() {
        if (loadedName.isEmpty()) {
            showInformation(IConstants.ERR_NOTHING_LOADED);
            return;
        }
        String pCopyName = JOptionPane.showInputDialog(
                this,
                IConstants.MSG_SAVE_COPY,
                IConstants.APP_TITLE,
                JOptionPane.QUESTION_MESSAGE);
        if (pCopyName == null) {
            return;
        }
        String pSourceName = loadedName;
        List<SynthEvent> pEvents = timelineModel.snapshot();
        executeAsync(
                () -> recordingService.saveEditedCopy(pSourceName, pCopyName, pEvents),
                pRecording -> {
                    loadedName = pRecording.name();
                    statusLabel.setText(String.format(IConstants.LBL_COPY_SAVED, pRecording.name()));
                    refreshRecordingList();
                },
                pFailure -> {
                    showFailure(pFailure);
                    if (pFailure instanceof InvalidNameException || pFailure instanceof DuplicateNameException) {
                        SwingUtilities.invokeLater(this::promptCopyName);
                    }
                });
    }

    private void showFailure(Throwable pFailure) {
        Throwable pCause = pFailure instanceof ExecutionException && pFailure.getCause() != null
                ? pFailure.getCause()
                : pFailure;
        String pMessage = pCause.getMessage();
        if (pMessage == null || pMessage.isBlank()) {
            pMessage = IConstants.ERR_UNKNOWN_RECORDING;
        }
        JOptionPane.showMessageDialog(
                this,
                pMessage,
                IConstants.MSG_ERROR_TITLE,
                JOptionPane.ERROR_MESSAGE);
    }

    private void showInformation(String pMessage) {
        JOptionPane.showMessageDialog(
                this,
                pMessage,
                IConstants.MSG_ERROR_TITLE,
                JOptionPane.INFORMATION_MESSAGE);
    }

    private <T> void executeAsync(BackgroundOperation<T> pOperation, Consumer<T> pSuccess, Consumer<Throwable> pFailure) {
        busy = true;
        recorderControlButton.setEnabled(false);
        pauseRecordingButton.setEnabled(false);
        saveCopyButton.setEnabled(false);
        new SwingWorker<T, Void>() {
            @Override
            protected T doInBackground() throws Exception {
                return pOperation.run();
            }

            @Override
            protected void done() {
                busy = false;
                setRecordingControls();
                saveCopyButton.setEnabled(!loadedName.isEmpty());
                try {
                    pSuccess.accept(get());
                } catch (InterruptedException pException) {
                    Thread.currentThread().interrupt();
                    pFailure.accept(pException);
                } catch (ExecutionException pException) {
                    pFailure.accept(pException.getCause() == null ? pException : pException.getCause());
                } catch (RuntimeException pException) {
                    pFailure.accept(pException);
                }
            }
        }.execute();
    }

    private interface BackgroundOperation<T> {

        T run() throws Exception;
    }

    private final class TimelineTableModel extends AbstractTableModel {

        private final List<SynthEvent> events = new ArrayList<>();
        private boolean dirty;
        private final String[] columnNames = {
                IConstants.COL_TIME,
                IConstants.COL_SYNTH,
                IConstants.COL_ACTION,
                IConstants.COL_NOTE,
                IConstants.COL_DURATION,
                IConstants.COL_VOLUME,
                IConstants.COL_TIMBRE_1,
                IConstants.COL_TIMBRE_2,
                IConstants.COL_TIMBRE_3
        };

        @Override
        public int getRowCount() {
            return events.size();
        }

        @Override
        public int getColumnCount() {
            return IConstants.TIMELINE_COLUMN_COUNT;
        }

        @Override
        public String getColumnName(int pColumn) {
            return columnNames[pColumn];
        }

        @Override
        public Object getValueAt(int pRowIndex, int pColumnIndex) {
            SynthEvent pEvent = events.get(pRowIndex);
            return switch (pColumnIndex) {
                case IConstants.TIMELINE_TIME_COLUMN -> Long.toString(pEvent.offsetMs());
                case IConstants.TIMELINE_SYNTH_COLUMN -> recordingService.synthName(pEvent.synthId());
                case IConstants.TIMELINE_ACTION_COLUMN -> actionName(pEvent.action());
                case IConstants.TIMELINE_NOTE_COLUMN -> Integer.toString(pEvent.note());
                case IConstants.TIMELINE_DURATION_COLUMN -> Integer.toString(pEvent.durMs());
                case IConstants.TIMELINE_VOLUME_COLUMN -> Float.toString(pEvent.velocity());
                case IConstants.TIMELINE_TIMBRE_1_COLUMN -> Float.toString(pEvent.timbre1());
                case IConstants.TIMELINE_TIMBRE_2_COLUMN -> Float.toString(pEvent.timbre2());
                case IConstants.TIMELINE_TIMBRE_3_COLUMN -> Float.toString(pEvent.timbre3());
                default -> IConstants.EMPTY_STRING;
            };
        }

        @Override
        public boolean isCellEditable(int pRowIndex, int pColumnIndex) {
            return pColumnIndex != IConstants.TIMELINE_SYNTH_COLUMN
                    && pColumnIndex != IConstants.TIMELINE_ACTION_COLUMN;
        }

        @Override
        public void setValueAt(Object pValue, int pRowIndex, int pColumnIndex) {
            try {
                SynthEvent pPrevious = events.get(pRowIndex);
                String pText = Objects.toString(pValue, IConstants.EMPTY_STRING).strip();
                SynthEvent pUpdated = switch (pColumnIndex) {
                    case IConstants.TIMELINE_TIME_COLUMN -> pPrevious.withOffset(Long.parseLong(pText));
                    case IConstants.TIMELINE_NOTE_COLUMN -> new SynthEvent(
                            pPrevious.offsetMs(), pPrevious.synthId(), pPrevious.action(),
                            Integer.parseInt(pText), pPrevious.durMs(), pPrevious.velocity(),
                            pPrevious.timbre1(), pPrevious.timbre2(), pPrevious.timbre3());
                    case IConstants.TIMELINE_DURATION_COLUMN -> new SynthEvent(
                            pPrevious.offsetMs(), pPrevious.synthId(), pPrevious.action(),
                            pPrevious.note(), Integer.parseInt(pText), pPrevious.velocity(),
                            pPrevious.timbre1(), pPrevious.timbre2(), pPrevious.timbre3());
                    case IConstants.TIMELINE_VOLUME_COLUMN -> new SynthEvent(
                            pPrevious.offsetMs(), pPrevious.synthId(), pPrevious.action(),
                            pPrevious.note(), pPrevious.durMs(), Float.parseFloat(pText),
                            pPrevious.timbre1(), pPrevious.timbre2(), pPrevious.timbre3());
                    case IConstants.TIMELINE_TIMBRE_1_COLUMN -> new SynthEvent(
                            pPrevious.offsetMs(), pPrevious.synthId(), pPrevious.action(),
                            pPrevious.note(), pPrevious.durMs(), pPrevious.velocity(),
                            Float.parseFloat(pText), pPrevious.timbre2(), pPrevious.timbre3());
                    case IConstants.TIMELINE_TIMBRE_2_COLUMN -> new SynthEvent(
                            pPrevious.offsetMs(), pPrevious.synthId(), pPrevious.action(),
                            pPrevious.note(), pPrevious.durMs(), pPrevious.velocity(),
                            pPrevious.timbre1(), Float.parseFloat(pText), pPrevious.timbre3());
                    case IConstants.TIMELINE_TIMBRE_3_COLUMN -> new SynthEvent(
                            pPrevious.offsetMs(), pPrevious.synthId(), pPrevious.action(),
                            pPrevious.note(), pPrevious.durMs(), pPrevious.velocity(),
                            pPrevious.timbre1(), pPrevious.timbre2(), Float.parseFloat(pText));
                    default -> pPrevious;
                };
                events.set(pRowIndex, pUpdated);
                dirty = true;
                fireTableCellUpdated(pRowIndex, pColumnIndex);
                statusLabel.setText(IConstants.MSG_EDITED_COPY_ONLY);
            } catch (NumberFormatException pException) {
                statusLabel.setText(IConstants.MSG_INVALID_EDIT);
            }
        }

        private void replaceEvents(List<SynthEvent> pEvents) {
            events.clear();
            events.addAll(pEvents);
            dirty = false;
            fireTableDataChanged();
        }

        private boolean isDirty() {
            return dirty;
        }

        private List<SynthEvent> snapshot() {
            return List.copyOf(events);
        }

        private String actionName(int pAction) {
            if (pAction < IConstants.ZERO_INT || pAction >= IConstants.ACTION_NAMES.length) {
                return Integer.toString(pAction);
            }
            return IConstants.ACTION_NAMES[pAction];
        }
    }
}

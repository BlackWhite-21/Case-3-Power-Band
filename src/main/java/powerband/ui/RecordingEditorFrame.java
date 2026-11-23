package powerband.ui;

import static powerband.util.IConstants.APP_TITLE;
import static powerband.util.IConstants.WINDOW_HEIGHT;
import static powerband.util.IConstants.WINDOW_WIDTH;

import javax.swing.JFrame;

import powerband.service.RecordingService;

public final class RecordingEditorFrame extends JFrame {

    public RecordingEditorFrame(RecordingService pRecordingService) {
        super(APP_TITLE);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setSize(WINDOW_WIDTH, WINDOW_HEIGHT);
        setContentPane(new RecordingEditorPanel(pRecordingService));
        setLocationByPlatform(true);
    }
}

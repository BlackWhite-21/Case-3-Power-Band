package powerband.repository;

import java.util.List;

<<<<<<< HEAD
=======
import powerband.dto.LoadedRecording;
>>>>>>> 610fc0b32360ae0d30055cdda2e4bfc30343954a
import powerband.dto.RecordingInfo;
import powerband.dto.SynthEvent;
import powerband.exception.RecordingException;
import powerband.exception.RecordingNotFoundException;

public interface RecordingRepository {

    void beginRecording(long pRecordedAtEpochMillis) throws RecordingException;

    void appendEvent(SynthEvent pEvent) throws RecordingException;

    void stopRecording() throws RecordingException;

    void discardRecording() throws RecordingException;

    RecordingInfo finalizeRecording(String pName) throws RecordingException;

    boolean containsName(String pName) throws RecordingException;

    long findHeaderPosition(String pName) throws RecordingException, RecordingNotFoundException;

    List<RecordingInfo> listRecordings() throws RecordingException;

<<<<<<< HEAD
    List<SynthEvent> readEvents(String pName) throws RecordingException, RecordingNotFoundException;

    RecordingInfo saveCopy(String pName, List<SynthEvent> pEvents, long pRecordedAtEpochMillis)
=======
    LoadedRecording loadRecording(String pName)
            throws RecordingException, RecordingNotFoundException;

    List<SynthEvent> readCommands(long pStartOffset, long pEndOffset)
>>>>>>> 610fc0b32360ae0d30055cdda2e4bfc30343954a
            throws RecordingException;
}

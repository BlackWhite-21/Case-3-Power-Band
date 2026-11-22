package powerband.repository;

import powerband.dto.RecordingInfo;
import powerband.dto.SynthEvent;
import powerband.exception.RecordingException;
import powerband.exception.RecordingNotFoundException;

public interface RecordingRepository {

    void beginRecording(long pRecordedAtEpochMillis) throws RecordingException;

    void appendEvent(SynthEvent pEvent) throws RecordingException;

    void stopRecording() throws RecordingException;

    RecordingInfo finalizeRecording(String pName) throws RecordingException;

    boolean containsName(String pName) throws RecordingException;

    long findHeaderPosition(String pName) throws RecordingException, RecordingNotFoundException;
}

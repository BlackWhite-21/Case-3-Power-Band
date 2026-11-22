package powerband.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static powerband.util.IConstants.ACTION_ON;
import static powerband.util.IConstants.ERR_LIST_IO;
import static powerband.util.IConstants.ERR_MUSIC_IO;
import static powerband.util.IConstants.INDEX_FILE;
import static powerband.util.IConstants.INDEX_RECORD_SIZE;
import static powerband.util.IConstants.LIST_FILE;
import static powerband.util.IConstants.MERGE_TAPES;
import static powerband.util.IConstants.MUSIC_FILE;
import static powerband.util.IConstants.MUSIC_RECORD_SIZE;
import static powerband.util.IConstants.NAME_LENGTH;
import static powerband.util.IConstants.RECORD_TYPE_COMMAND;
import static powerband.util.IConstants.RECORD_TYPE_HEADER;
import static powerband.util.IConstants.SYNTH_SAXOPHONE_ID;
import static powerband.util.IConstants.ZERO_INT;
import static powerband.util.IConstants.ZERO_LONG;

import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import powerband.dto.RecordingInfo;
import powerband.dto.SynthEvent;
import powerband.exception.DuplicateNameException;
import powerband.exception.RecordingException;
import powerband.exception.RecordingNotFoundException;

class BinaryRecordingRepositoryTest {

    @TempDir
    private Path temporaryDirectory;

    private Path dataDirectory;
    private BinaryRecordingRepository repository;

    @BeforeEach
    void setUp() {
        dataDirectory = temporaryDirectory.resolve("recording-data");
        repository = new BinaryRecordingRepository(dataDirectory);
    }

    @Test
    void writesFixedSizeHeaderCommandsIndexAndChronologicalList() throws Exception {
        long pRecordedAt = Instant.parse("2026-11-22T18:00:00Z").toEpochMilli();
        SynthEvent pEvent = new SynthEvent(125, SYNTH_SAXOPHONE_ID, ACTION_ON,
                60, 0, 0.75f, 0.1f, 0.2f, 0.3f);

        repository.beginRecording(pRecordedAt);
        repository.appendEvent(pEvent);
        repository.stopRecording();
        RecordingInfo pInfo = repository.finalizeRecording("concierto");

        assertEquals(MUSIC_RECORD_SIZE * MERGE_TAPES, Files.size(dataDirectory.resolve(MUSIC_FILE)));
        assertEquals(INDEX_RECORD_SIZE, Files.size(dataDirectory.resolve(INDEX_FILE)));
        assertEquals("concierto|" + pInfo.recordedAt(),
                Files.readString(dataDirectory.resolve(LIST_FILE), StandardCharsets.UTF_8).strip());
        assertEquals(0L, repository.findHeaderPosition("concierto"));

        try (RandomAccessFile pMusic = new RandomAccessFile(dataDirectory.resolve(MUSIC_FILE).toFile(), "r")) {
            assertEquals(RECORD_TYPE_HEADER, pMusic.readByte());
            assertEquals("concierto", readFixedName(pMusic));
            assertEquals(0L, pMusic.readLong());
            assertEquals((long) MUSIC_RECORD_SIZE * MERGE_TAPES, pMusic.readLong());
            assertEquals(pRecordedAt, pMusic.readLong());

            pMusic.seek(MUSIC_RECORD_SIZE);
            assertEquals(RECORD_TYPE_COMMAND, pMusic.readByte());
            assertEquals(125L, pMusic.readLong());
            assertEquals(SYNTH_SAXOPHONE_ID, pMusic.readInt());
            assertEquals(ACTION_ON, pMusic.readInt());
            assertEquals(60, pMusic.readInt());
            assertEquals(ZERO_INT, pMusic.readInt());
            assertEquals(0.75f, pMusic.readFloat(), 0.0001f);
            assertEquals(0.1f, pMusic.readFloat(), 0.0001f);
            assertEquals(0.2f, pMusic.readFloat(), 0.0001f);
            assertEquals(0.3f, pMusic.readFloat(), 0.0001f);
        }
    }

    @Test
    void balancedMergeSortOrdersDiskIndexAndListKeepsRecordingOrder() throws Exception {
        List<String> pRecordingOrder = List.of("delta", "alpha", "echo", "bravo", "charlie");
        List<String> pSortedNames = new ArrayList<>(pRecordingOrder);
        pSortedNames.sort(String::compareTo);
        List<String> pSavedNames = new ArrayList<>();
        long pRecordedAt = Instant.parse("2026-11-22T18:00:00Z").toEpochMilli();
        List<String> pExpectedList = new ArrayList<>();

        for (String pName : pRecordingOrder) {
            repository.beginRecording(pRecordedAt);
            repository.stopRecording();
            RecordingInfo pInfo = repository.finalizeRecording(pName);
            pExpectedList.add(pName + "|" + pInfo.recordedAt());
            pSavedNames.add(pName);
            List<String> pExpectedSortedPrefix = new ArrayList<>(pSavedNames);
            pExpectedSortedPrefix.sort(String::compareTo);
            assertEquals(pExpectedSortedPrefix, readIndexNames());
            pRecordedAt++;
        }

        assertEquals(pSortedNames, readIndexNames());
        assertEquals(pRecordingOrder, Files.readAllLines(dataDirectory.resolve(LIST_FILE), StandardCharsets.UTF_8)
                .stream()
                .map(pLine -> pLine.substring(ZERO_INT, pLine.indexOf('|')))
                .toList());
        assertEquals(pExpectedList, Files.readAllLines(dataDirectory.resolve(LIST_FILE), StandardCharsets.UTF_8));
        for (String pName : pRecordingOrder) {
            assertTrue(repository.containsName(pName));
        }
    }

    @Test
    void duplicateNameKeepsStoppedRecordingAvailableForRetry() throws Exception {
        saveRecording("duplicado");
        repository.beginRecording(Instant.now().toEpochMilli());
        repository.stopRecording();

        assertThrows(DuplicateNameException.class, () -> repository.finalizeRecording("duplicado"));
        assertEquals(ZERO_LONG, repository.findHeaderPosition("duplicado"));
        repository.finalizeRecording("nuevo");
        assertEquals(MUSIC_RECORD_SIZE, repository.findHeaderPosition("nuevo"));
        assertTrue(repository.containsName("nuevo"));
    }

    @Test
    void findsSavedRecordingAfterRepositoryIsReopened() throws Exception {
        saveRecording("persistida");
        BinaryRecordingRepository pReopenedRepository = new BinaryRecordingRepository(dataDirectory);

        assertEquals(ZERO_LONG, pReopenedRepository.findHeaderPosition("persistida"));
    }

    @Test
    void failedListWriteDoesNotReportSuccessAndKeepsRecordingAvailableForRetry() throws Exception {
        Files.createDirectories(dataDirectory);
        Files.createDirectory(dataDirectory.resolve(LIST_FILE));
        repository.beginRecording(Instant.now().toEpochMilli());
        repository.stopRecording();

        RecordingException pException = assertThrows(
                RecordingException.class,
                () -> repository.finalizeRecording("fallida"));

        assertEquals(ERR_LIST_IO, pException.getMessage());
        assertFalse(repository.containsName("fallida"));
        try (RandomAccessFile pMusic = new RandomAccessFile(dataDirectory.resolve(MUSIC_FILE).toFile(), "r")) {
            assertEquals(RECORD_TYPE_HEADER, pMusic.readByte());
            assertEquals("", readFixedName(pMusic));
        }

        Files.delete(dataDirectory.resolve(LIST_FILE));
        repository.finalizeRecording("reintento");
        assertTrue(repository.containsName("reintento"));
    }

    @Test
    void reportsMissingNamesAndRejectsBrokenMusicFile() throws Exception {
        assertThrows(RecordingNotFoundException.class, () -> repository.findHeaderPosition("ausente"));

        Path pInvalidDataDirectory = temporaryDirectory.resolve("invalid-data");
        Files.createDirectories(pInvalidDataDirectory);
        Files.createDirectory(pInvalidDataDirectory.resolve(MUSIC_FILE));
        BinaryRecordingRepository pInvalidRepository = new BinaryRecordingRepository(pInvalidDataDirectory);
        RecordingException pException = assertThrows(
                RecordingException.class,
                () -> pInvalidRepository.beginRecording(Instant.now().toEpochMilli()));

        assertEquals(ERR_MUSIC_IO, pException.getMessage());
    }

    private RecordingInfo saveRecording(String pName) throws Exception {
        repository.beginRecording(Instant.now().toEpochMilli());
        repository.stopRecording();
        return repository.finalizeRecording(pName);
    }

    private List<String> readIndexNames() throws Exception {
        List<String> pIndexNames = new ArrayList<>();
        try (RandomAccessFile pIndex = new RandomAccessFile(dataDirectory.resolve(INDEX_FILE).toFile(), "r")) {
            while (pIndex.getFilePointer() < pIndex.length()) {
                pIndexNames.add(RecordingFileFormat.readIndexEntry(pIndex).name());
            }
        }
        return pIndexNames;
    }

    private String readFixedName(RandomAccessFile pInput) throws Exception {
        StringBuilder pName = new StringBuilder(NAME_LENGTH);
        boolean pPaddingStarted = false;
        for (int pIndex = ZERO_INT; pIndex < NAME_LENGTH; pIndex++) {
            char pCharacter = pInput.readChar();
            if (pCharacter == 0) {
                pPaddingStarted = true;
            } else if (!pPaddingStarted) {
                pName.append(pCharacter);
            }
        }
        return pName.toString();
    }
}

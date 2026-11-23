package powerband.repository;

import static java.nio.file.StandardCopyOption.ATOMIC_MOVE;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static java.nio.file.StandardOpenOption.APPEND;
import static java.nio.file.StandardOpenOption.CREATE;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import powerband.dto.RecordingInfo;
import powerband.dto.SynthEvent;
import powerband.dto.LoadedRecording;
import powerband.exception.DuplicateNameException;
import powerband.exception.RecordingException;
import powerband.exception.RecordingNotFoundException;
import powerband.util.IConstants;

public final class BinaryRecordingRepository implements RecordingRepository {

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern(IConstants.DATE_PATTERN, Locale.ROOT);

    private final Path dataDirectory;
    private final BalancedMergeSort mergeSort = new BalancedMergeSort();
    private long headerPosition = IConstants.NOT_FOUND;
    private long recordedAtEpochMillis;
    private boolean recording;
    private boolean pendingName;

    public BinaryRecordingRepository() {
        this(Path.of(IConstants.DATA_DIR));
    }

    public BinaryRecordingRepository(Path pDataDirectory) {
        dataDirectory = Objects.requireNonNull(pDataDirectory);
    }

    @Override
    public synchronized void beginRecording(long pRecordedAtEpochMillis) throws RecordingException {
        if (recording || pendingName) {
            throw new RecordingException(IConstants.ERR_ALREADY_RECORDING);
        }
        Path pMusicPath = dataDirectory.resolve(IConstants.MUSIC_FILE);
        try {
            Files.createDirectories(dataDirectory);
            try (RandomAccessFile pMusic = new RandomAccessFile(pMusicPath.toFile(), IConstants.FILE_MODE_RW)) {
                long pHeaderPosition = pMusic.length();
                pMusic.seek(pHeaderPosition);
                RecordingFileFormat.writeHeader(
                        pMusic,
                        IConstants.EMPTY_STRING,
                        pHeaderPosition,
                        pHeaderPosition,
                        pRecordedAtEpochMillis);
                headerPosition = pHeaderPosition;
                recordedAtEpochMillis = pRecordedAtEpochMillis;
                recording = true;
            }
        } catch (IOException pException) {
            throw new RecordingException(IConstants.ERR_MUSIC_IO, pException);
        }
    }

    @Override
    public synchronized void appendEvent(SynthEvent pEvent) throws RecordingException {
        if (!recording) {
            throw new RecordingException(IConstants.ERR_NOT_RECORDING);
        }
        Path pMusicPath = dataDirectory.resolve(IConstants.MUSIC_FILE);
        try (RandomAccessFile pMusic = new RandomAccessFile(pMusicPath.toFile(), IConstants.FILE_MODE_RW)) {
            pMusic.seek(pMusic.length());
            RecordingFileFormat.writeCommand(pMusic, Objects.requireNonNull(pEvent));
        } catch (IOException pException) {
            throw new RecordingException(IConstants.ERR_MUSIC_IO, pException);
        }
    }

    @Override
    public synchronized void stopRecording() throws RecordingException {
        if (!recording) {
            throw new RecordingException(IConstants.ERR_NOT_RECORDING);
        }
        recording = false;
        pendingName = true;
    }

    @Override
    public synchronized void discardRecording() throws RecordingException {
        if (!recording && !pendingName) {
            throw new RecordingException(IConstants.ERR_NOT_RECORDING);
        }
        Path pMusicPath = dataDirectory.resolve(IConstants.MUSIC_FILE);
        try (RandomAccessFile pMusic = new RandomAccessFile(pMusicPath.toFile(), IConstants.FILE_MODE_RW)) {
            if (headerPosition < IConstants.ZERO_LONG
                    || headerPosition > pMusic.length()
                    || pMusic.length() < headerPosition + IConstants.MUSIC_RECORD_SIZE) {
                throw new IOException(IConstants.ERR_MUSIC_IO);
            }
            pMusic.setLength(headerPosition);
            resetPendingRecording();
            recording = false;
        } catch (IOException pException) {
            throw new RecordingException(IConstants.ERR_MUSIC_IO, pException);
        }
    }

    @Override
    public synchronized RecordingInfo finalizeRecording(String pName) throws RecordingException {
        if (!pendingName) {
            throw new RecordingException(IConstants.ERR_NOT_RECORDING);
        }
        if (containsName(pName)) {
            throw new DuplicateNameException(String.format(IConstants.ERR_NAME_DUPLICATE, pName));
        }

        Path pMusicPath = dataDirectory.resolve(IConstants.MUSIC_FILE);
        Path pIndexPath = dataDirectory.resolve(IConstants.INDEX_FILE);
        Path pIndexWorkPath = dataDirectory.resolve(IConstants.INDEX_FILE + IConstants.SORT_TAPE_PREFIX);
        Path pListPath = dataDirectory.resolve(IConstants.LIST_FILE);
        boolean pListExisted = Files.exists(pListPath);
        boolean pListWasFile = Files.isRegularFile(pListPath);
        long pListOriginalLength = IConstants.ZERO_LONG;
        boolean pListMayHaveChanged = false;
        try {
            if (pListWasFile) {
                pListOriginalLength = Files.size(pListPath);
            }
            if (Files.exists(pIndexPath)) {
                Files.copy(pIndexPath, pIndexWorkPath, REPLACE_EXISTING);
            } else {
                Files.deleteIfExists(pIndexWorkPath);
                Files.createFile(pIndexWorkPath);
            }
            try (RandomAccessFile pIndex = new RandomAccessFile(
                    pIndexWorkPath.toFile(), IConstants.FILE_MODE_RW)) {
                pIndex.seek(pIndex.length());
                RecordingFileFormat.writeIndexEntry(pIndex, pName, headerPosition);
            }
            try {
                mergeSort.sort(pIndexWorkPath);
            } catch (IOException pException) {
                throw new RecordingException(IConstants.ERR_SORT_IO, pException);
            }

            pListMayHaveChanged = true;
            appendListEntry(pListPath, pName, recordedAtEpochMillis);
            updateHeader(pMusicPath, pName);
            replaceFile(pIndexWorkPath, pIndexPath);
            RecordingInfo pRecordingInfo = new RecordingInfo(
                    pName,
                    DATE_FORMATTER.format(Instant.ofEpochMilli(recordedAtEpochMillis)
                            .atZone(ZoneId.systemDefault())));
            headerPosition = IConstants.NOT_FOUND;
            pendingName = false;
            return pRecordingInfo;
        } catch (RecordingException pException) {
            rollback(pMusicPath, pListPath, pIndexWorkPath, pListExisted, pListWasFile,
                    pListOriginalLength, pListMayHaveChanged, pException);
            throw pException;
        } catch (IOException pException) {
            RecordingException pRecordingException =
                    new RecordingException(IConstants.ERR_INDEX_IO, pException);
            rollback(pMusicPath, pListPath, pIndexWorkPath, pListExisted, pListWasFile,
                    pListOriginalLength, pListMayHaveChanged, pRecordingException);
            throw pRecordingException;
        }
    }

    @Override
    public synchronized boolean containsName(String pName) throws RecordingException {
        try {
            findHeaderPosition(pName);
            return true;
        } catch (RecordingNotFoundException pException) {
            return false;
        }
    }

    @Override
    public synchronized long findHeaderPosition(String pName)
            throws RecordingException, RecordingNotFoundException {
        Path pIndexPath = dataDirectory.resolve(IConstants.INDEX_FILE);
        if (!Files.exists(pIndexPath)) {
            throw new RecordingNotFoundException(String.format(IConstants.ERR_NOT_FOUND, pName));
        }
        try (RandomAccessFile pIndex = new RandomAccessFile(pIndexPath.toFile(), IConstants.FILE_MODE_R)) {
            if (pIndex.length() % IConstants.INDEX_RECORD_SIZE != IConstants.ZERO_LONG) {
                throw new IOException(IConstants.ERR_INDEX_IO);
            }
            long pLow = IConstants.ZERO_LONG;
            long pHigh = pIndex.length() / IConstants.INDEX_RECORD_SIZE - 1;
            while (pLow <= pHigh) {
                long pMiddle = pLow + (pHigh - pLow) / IConstants.MERGE_TAPES;
                pIndex.seek(pMiddle * IConstants.INDEX_RECORD_SIZE);
                RecordingFileFormat.IndexEntry pEntry = RecordingFileFormat.readIndexEntry(pIndex);
                int pComparison = pEntry.name().compareTo(pName);
                if (pComparison == IConstants.ZERO_INT) {
                    return pEntry.headerPosition();
                }
                if (pComparison < IConstants.ZERO_INT) {
                    pLow = pMiddle + 1;
                } else {
                    pHigh = pMiddle - 1;
                }
            }
            throw new RecordingNotFoundException(String.format(IConstants.ERR_NOT_FOUND, pName));
        } catch (RecordingNotFoundException pException) {
            throw pException;
        } catch (IOException pException) {
            throw new RecordingException(IConstants.ERR_INDEX_IO, pException);
        }
    }

    @Override
    public synchronized List<RecordingInfo> listRecordings() throws RecordingException {
        Path pListPath = dataDirectory.resolve(IConstants.LIST_FILE);
        if (!Files.exists(pListPath)) {
<<<<<<< HEAD
            return List.of();
        }
        try {
            List<String> pLines = Files.readAllLines(pListPath, StandardCharsets.UTF_8);
            List<RecordingInfo> pRecordings = new ArrayList<>(pLines.size());
            for (String pLine : pLines) {
                String[] pFields = pLine.split(IConstants.LIST_SEPARATOR_REGEX, IConstants.LIST_FIELDS);
                if (pFields.length != IConstants.LIST_FIELDS
                        || pFields[IConstants.ZERO_INT].isBlank()
                        || pFields[IConstants.LIST_FIELDS - 1].isBlank()) {
                    throw new IOException(IConstants.ERR_LIST_IO);
                }
                pRecordings.add(new RecordingInfo(
                        pFields[IConstants.ZERO_INT],
                        pFields[IConstants.LIST_FIELDS - 1]));
            }
            return List.copyOf(pRecordings);
=======
            return new ArrayList<>();
        }
        try {
            List<RecordingInfo> pRecordings = new ArrayList<>();
            List<String> pLines = Files.readAllLines(pListPath, StandardCharsets.UTF_8);
            for (String pLine : pLines) {
                if (pLine.isBlank()) {
                    continue;
                }
                String[] pFields = pLine.split(IConstants.LIST_SEPARATOR_REGEX, IConstants.LIST_FIELDS);
                if (pFields.length != IConstants.LIST_FIELDS) {
                    throw new RecordingException(IConstants.ERR_LIST_LINE);
                }
                pRecordings.add(new RecordingInfo(pFields[0], pFields[1]));
            }
            return pRecordings;
>>>>>>> 610fc0b32360ae0d30055cdda2e4bfc30343954a
        } catch (IOException pException) {
            throw new RecordingException(IConstants.ERR_LIST_IO, pException);
        }
    }

    @Override
<<<<<<< HEAD
    public synchronized List<SynthEvent> readEvents(String pName)
            throws RecordingException, RecordingNotFoundException {
        long pHeaderPosition = findHeaderPosition(pName);
        Path pMusicPath = dataDirectory.resolve(IConstants.MUSIC_FILE);
        try (RandomAccessFile pMusic = new RandomAccessFile(pMusicPath.toFile(), IConstants.FILE_MODE_R)) {
            long pMusicLength = pMusic.length();
            if (pHeaderPosition < IConstants.ZERO_LONG
                    || pHeaderPosition % IConstants.MUSIC_RECORD_SIZE != IConstants.ZERO_LONG
                    || pMusicLength % IConstants.MUSIC_RECORD_SIZE != IConstants.ZERO_LONG
                    || pHeaderPosition > pMusicLength - IConstants.MUSIC_RECORD_SIZE) {
                throw new IOException(IConstants.ERR_MUSIC_IO);
            }
            pMusic.seek(pHeaderPosition);
            RecordingFileFormat.HeaderEntry pHeader = RecordingFileFormat.readHeader(pMusic);
            long pRecordingLength = pHeader.endOffset() - pHeader.startOffset();
            if (!pHeader.name().equals(pName)
                    || pHeader.startOffset() != pHeaderPosition
                    || pHeader.endOffset() < pHeader.startOffset()
                    || pRecordingLength < IConstants.MUSIC_RECORD_SIZE
                    || pRecordingLength % IConstants.MUSIC_RECORD_SIZE != IConstants.ZERO_LONG
                    || pHeader.endOffset() > pMusicLength) {
                throw new IOException(IConstants.ERR_MUSIC_IO);
            }

            List<SynthEvent> pEvents = new ArrayList<>();
            long pCommandOffset = pHeaderPosition + IConstants.MUSIC_RECORD_SIZE;
            while (pCommandOffset < pHeader.endOffset()) {
                pMusic.seek(pCommandOffset);
                pEvents.add(RecordingFileFormat.readCommand(pMusic));
                pCommandOffset += IConstants.MUSIC_RECORD_SIZE;
            }
            return List.copyOf(pEvents);
=======
    public synchronized LoadedRecording loadRecording(String pName)
            throws RecordingException, RecordingNotFoundException {
        Objects.requireNonNull(pName);
        long pHeaderPosition = findHeaderPosition(pName);
        Path pMusicPath = dataDirectory.resolve(IConstants.MUSIC_FILE);
        try (RandomAccessFile pMusic = new RandomAccessFile(pMusicPath.toFile(), IConstants.FILE_MODE_R)) {
            if (pHeaderPosition < IConstants.ZERO_LONG
                    || pHeaderPosition % IConstants.MUSIC_RECORD_SIZE != IConstants.ZERO_LONG
                    || pHeaderPosition >= pMusic.length()) {
                throw new RecordingException(IConstants.ERR_PLAYBACK_HEADER);
            }
            pMusic.seek(pHeaderPosition);
            RecordingFileFormat.HeaderEntry pHeader = RecordingFileFormat.readHeader(pMusic);
            if (!pHeader.name().equals(pName)
                    || pHeader.startOffset() != pHeaderPosition
                    || pHeader.endOffset() < pHeaderPosition + IConstants.MUSIC_RECORD_SIZE
                    || pHeader.endOffset() > pMusic.length()
                    || pHeader.endOffset() % IConstants.MUSIC_RECORD_SIZE != IConstants.ZERO_LONG) {
                throw new RecordingException(IConstants.ERR_PLAYBACK_HEADER);
            }
            return new LoadedRecording(
                    pHeader.name(),
                    pHeader.startOffset(),
                    pHeader.endOffset(),
                    pHeader.recordedAtEpochMillis());
>>>>>>> 610fc0b32360ae0d30055cdda2e4bfc30343954a
        } catch (IOException pException) {
            throw new RecordingException(IConstants.ERR_MUSIC_IO, pException);
        }
    }

    @Override
<<<<<<< HEAD
    public synchronized RecordingInfo saveCopy(
            String pName,
            List<SynthEvent> pEvents,
            long pRecordedAtEpochMillis) throws RecordingException {
        Objects.requireNonNull(pName);
        Objects.requireNonNull(pEvents);
        for (SynthEvent pEvent : pEvents) {
            Objects.requireNonNull(pEvent);
        }
        if (recording || pendingName) {
            throw new RecordingException(IConstants.ERR_RECORDING_BUSY);
        }
        if (containsName(pName)) {
            throw new DuplicateNameException(String.format(IConstants.ERR_NAME_DUPLICATE, pName));
        }

        Path pMusicPath = dataDirectory.resolve(IConstants.MUSIC_FILE);
        long pOriginalMusicLength = IConstants.ZERO_LONG;
        try {
            Files.createDirectories(dataDirectory);
            if (Files.exists(pMusicPath)) {
                pOriginalMusicLength = Files.size(pMusicPath);
                if (pOriginalMusicLength % IConstants.MUSIC_RECORD_SIZE != IConstants.ZERO_LONG) {
                    throw new IOException(IConstants.ERR_MUSIC_IO);
                }
            }
            headerPosition = pOriginalMusicLength;
            recordedAtEpochMillis = pRecordedAtEpochMillis;
            try (RandomAccessFile pMusic = new RandomAccessFile(pMusicPath.toFile(), IConstants.FILE_MODE_RW)) {
                pMusic.seek(headerPosition);
                RecordingFileFormat.writeHeader(
                        pMusic,
                        IConstants.EMPTY_STRING,
                        headerPosition,
                        headerPosition,
                        pRecordedAtEpochMillis);
                for (SynthEvent pEvent : pEvents) {
                    RecordingFileFormat.writeCommand(pMusic, Objects.requireNonNull(pEvent));
                }
            }
            pendingName = true;
            return finalizeRecording(pName);
        } catch (RecordingException pException) {
            truncateMusic(pMusicPath, pOriginalMusicLength, pException);
            resetPendingRecording();
            throw pException;
        } catch (IOException pException) {
            RecordingException pRecordingException =
                    new RecordingException(IConstants.ERR_MUSIC_IO, pException);
            truncateMusic(pMusicPath, pOriginalMusicLength, pRecordingException);
            resetPendingRecording();
            throw pRecordingException;
=======
    public synchronized List<SynthEvent> readCommands(long pStartOffset, long pEndOffset)
            throws RecordingException {
        if (pStartOffset < IConstants.ZERO_LONG
                || pEndOffset < pStartOffset
                || pStartOffset % IConstants.MUSIC_RECORD_SIZE != IConstants.ZERO_LONG
                || pEndOffset % IConstants.MUSIC_RECORD_SIZE != IConstants.ZERO_LONG) {
            throw new RecordingException(IConstants.ERR_MUSIC_IO);
        }
        Path pMusicPath = dataDirectory.resolve(IConstants.MUSIC_FILE);
        try (RandomAccessFile pMusic = new RandomAccessFile(pMusicPath.toFile(), IConstants.FILE_MODE_R)) {
            if (pEndOffset > pMusic.length()) {
                throw new RecordingException(IConstants.ERR_MUSIC_IO);
            }
            pMusic.seek(pStartOffset);
            List<SynthEvent> pEvents = new ArrayList<>(IConstants.PLAYBACK_BUFFER_SIZE);
            while (pMusic.getFilePointer() < pEndOffset
                    && pEvents.size() < IConstants.PLAYBACK_BUFFER_SIZE) {
                SynthEvent pEvent = RecordingFileFormat.readCommand(pMusic);
                pEvents.add(pEvent);
                pMusic.seek(pMusic.getFilePointer()
                        + IConstants.MUSIC_RECORD_SIZE
                        - IConstants.MUSIC_COMMAND_FIXED_BYTES);
            }
            return pEvents;
        } catch (IOException pException) {
            throw new RecordingException(IConstants.ERR_MUSIC_IO, pException);
>>>>>>> 610fc0b32360ae0d30055cdda2e4bfc30343954a
        }
    }

    private void appendListEntry(Path pListPath, String pName, long pRecordedAtEpochMillis)
            throws RecordingException {
        String pDate = DATE_FORMATTER.format(
                Instant.ofEpochMilli(pRecordedAtEpochMillis).atZone(ZoneId.systemDefault()));
        try {
            Files.writeString(
                    pListPath,
                    pName + IConstants.LIST_SEPARATOR + pDate + System.lineSeparator(),
                    StandardCharsets.UTF_8,
                    CREATE,
                    APPEND);
        } catch (IOException pException) {
            throw new RecordingException(IConstants.ERR_LIST_IO, pException);
        }
    }

    private void updateHeader(Path pMusicPath, String pName) throws RecordingException {
        try (RandomAccessFile pMusic = new RandomAccessFile(pMusicPath.toFile(), IConstants.FILE_MODE_RW)) {
            long pEndOffset = pMusic.length();
            pMusic.seek(headerPosition);
            RecordingFileFormat.writeHeader(pMusic, pName, headerPosition, pEndOffset, recordedAtEpochMillis);
        } catch (IOException pException) {
            throw new RecordingException(IConstants.ERR_MUSIC_IO, pException);
        }
    }

    private void rollback(
            Path pMusicPath,
            Path pListPath,
            Path pIndexWorkPath,
            boolean pListExisted,
            boolean pListWasFile,
            long pListOriginalLength,
            boolean pListMayHaveChanged,
            RecordingException pFailure) {
        try {
            if (pListMayHaveChanged && (!pListExisted || pListWasFile)) {
                if (pListExisted) {
                    try (RandomAccessFile pList = new RandomAccessFile(
                            pListPath.toFile(), IConstants.FILE_MODE_RW)) {
                        pList.setLength(pListOriginalLength);
                    }
                } else {
                    Files.deleteIfExists(pListPath);
                }
            }
        } catch (IOException pRollbackException) {
            pFailure.addSuppressed(pRollbackException);
        }
        try (RandomAccessFile pMusic = new RandomAccessFile(pMusicPath.toFile(), IConstants.FILE_MODE_RW)) {
            pMusic.seek(headerPosition);
            RecordingFileFormat.writeHeader(
                    pMusic,
                    IConstants.EMPTY_STRING,
                    headerPosition,
                    headerPosition,
                    recordedAtEpochMillis);
        } catch (IOException pRollbackException) {
            pFailure.addSuppressed(pRollbackException);
        }
        try {
            Files.deleteIfExists(pIndexWorkPath);
        } catch (IOException pRollbackException) {
            pFailure.addSuppressed(pRollbackException);
        }
    }

    private void truncateMusic(Path pMusicPath, long pLength, RecordingException pFailure) {
        try (RandomAccessFile pMusic = new RandomAccessFile(pMusicPath.toFile(), IConstants.FILE_MODE_RW)) {
            pMusic.setLength(pLength);
        } catch (IOException pRollbackException) {
            pFailure.addSuppressed(pRollbackException);
        }
    }

    private void resetPendingRecording() {
        headerPosition = IConstants.NOT_FOUND;
        recordedAtEpochMillis = IConstants.ZERO_LONG;
        pendingName = false;
    }

    private void replaceFile(Path pSource, Path pTarget) throws IOException {
        try {
            Files.move(pSource, pTarget, REPLACE_EXISTING, ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException pException) {
            Files.move(pSource, pTarget, REPLACE_EXISTING);
        }
    }
}

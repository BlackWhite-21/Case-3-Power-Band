package powerband.repository;

import static java.nio.file.StandardCopyOption.ATOMIC_MOVE;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;

import powerband.util.IConstants;

final class BalancedMergeSort {

    void sort(Path pIndexFile) throws IOException {
        long pFileLength = Files.exists(pIndexFile) ? Files.size(pIndexFile) : IConstants.ZERO_LONG;
        if (pFileLength % IConstants.INDEX_RECORD_SIZE != IConstants.ZERO_LONG) {
            throw new IOException(IConstants.ERR_INDEX_IO);
        }
        long pRecordCount = pFileLength / IConstants.INDEX_RECORD_SIZE;
        if (pRecordCount < IConstants.MERGE_TAPES) {
            return;
        }

        Path pDataDirectory = pIndexFile.getParent();
        Path[] pSourceTapes = tapePaths(pDataDirectory, IConstants.SORT_TAPES_A);
        Path[] pDestinationTapes = tapePaths(pDataDirectory, IConstants.SORT_TAPES_B);
        Path pSortedFile = pIndexFile.resolveSibling(
                pIndexFile.getFileName().toString() + IConstants.SORT_TAPE_PREFIX);
        try {
            distribute(pIndexFile, pSourceTapes, IConstants.FIRST_RUN_LENGTH);
            long pRunLength = IConstants.FIRST_RUN_LENGTH;
            while (pRunLength < pRecordCount) {
                mergePass(pSourceTapes, pDestinationTapes, pRunLength);
                Path[] pSwapTapes = pSourceTapes;
                pSourceTapes = pDestinationTapes;
                pDestinationTapes = pSwapTapes;
                pRunLength = Math.min(pRecordCount, pRunLength * IConstants.MERGE_TAPES);
            }
            writeSortedFile(pSourceTapes, pSortedFile);
            replaceFile(pSortedFile, pIndexFile);
        } finally {
            deleteTapes(pDataDirectory);
            Files.deleteIfExists(pSortedFile);
        }
    }

    private void distribute(Path pIndexFile, Path[] pTapes, long pRunLength) throws IOException {
        truncateTapes(pTapes);
        try (RandomAccessFile pInput = new RandomAccessFile(pIndexFile.toFile(), IConstants.FILE_MODE_R);
                RandomAccessFile pFirstTape = new RandomAccessFile(pTapes[0].toFile(), IConstants.FILE_MODE_RW);
                RandomAccessFile pSecondTape = new RandomAccessFile(pTapes[1].toFile(), IConstants.FILE_MODE_RW)) {
            RandomAccessFile[] pOutputs = { pFirstTape, pSecondTape };
            long pRunNumber = IConstants.ZERO_LONG;
            while (pInput.getFilePointer() < pInput.length()) {
                RandomAccessFile pOutput = pOutputs[(int) (pRunNumber % IConstants.MERGE_TAPES)];
                long pRemaining = pRunLength;
                while (pRemaining > IConstants.ZERO_LONG && pInput.getFilePointer() < pInput.length()) {
                    RecordingFileFormat.IndexEntry pEntry = RecordingFileFormat.readIndexEntry(pInput);
                    RecordingFileFormat.writeIndexEntry(pOutput, pEntry.name(), pEntry.headerPosition());
                    pRemaining--;
                }
                pRunNumber++;
            }
        }
    }

    private void mergePass(Path[] pSources, Path[] pDestinations, long pRunLength) throws IOException {
        truncateTapes(pDestinations);
        try (RandomAccessFile pSource0 = new RandomAccessFile(pSources[0].toFile(), IConstants.FILE_MODE_R);
                RandomAccessFile pSource1 = new RandomAccessFile(pSources[1].toFile(), IConstants.FILE_MODE_R);
                RandomAccessFile pDestination0 = new RandomAccessFile(
                        pDestinations[0].toFile(), IConstants.FILE_MODE_RW);
                RandomAccessFile pDestination1 = new RandomAccessFile(
                        pDestinations[1].toFile(), IConstants.FILE_MODE_RW)) {
            RandomAccessFile[] pInputs = { pSource0, pSource1 };
            RandomAccessFile[] pOutputs = { pDestination0, pDestination1 };
            long[] pRemainingRecords = {
                pSource0.length() / IConstants.INDEX_RECORD_SIZE,
                pSource1.length() / IConstants.INDEX_RECORD_SIZE
            };
            long pRunNumber = IConstants.ZERO_LONG;
            while (pRemainingRecords[0] > IConstants.ZERO_LONG
                    || pRemainingRecords[1] > IConstants.ZERO_LONG) {
                long pLeftCount = Math.min(pRunLength, pRemainingRecords[0]);
                long pRightCount = Math.min(pRunLength, pRemainingRecords[1]);
                mergeRuns(
                        pInputs[0],
                        pInputs[1],
                        pOutputs[(int) (pRunNumber % IConstants.MERGE_TAPES)],
                        pLeftCount,
                        pRightCount);
                pRemainingRecords[0] -= pLeftCount;
                pRemainingRecords[1] -= pRightCount;
                pRunNumber++;
            }
        }
    }

    private void mergeRuns(
            RandomAccessFile pLeftInput,
            RandomAccessFile pRightInput,
            RandomAccessFile pOutput,
            long pLeftCount,
            long pRightCount) throws IOException {
        RecordingFileFormat.IndexEntry pLeft = pLeftCount > IConstants.ZERO_LONG
                ? RecordingFileFormat.readIndexEntry(pLeftInput)
                : null;
        RecordingFileFormat.IndexEntry pRight = pRightCount > IConstants.ZERO_LONG
                ? RecordingFileFormat.readIndexEntry(pRightInput)
                : null;
        if ((pLeftCount > IConstants.ZERO_LONG && pLeft == null)
                || (pRightCount > IConstants.ZERO_LONG && pRight == null)) {
            throw new IOException(IConstants.ERR_INDEX_IO);
        }
        while (pLeftCount > IConstants.ZERO_LONG && pRightCount > IConstants.ZERO_LONG) {
            if (pLeft == null || pRight == null) {
                throw new IOException(IConstants.ERR_INDEX_IO);
            }
            if (pLeft.name().compareTo(pRight.name()) <= IConstants.ZERO_INT) {
                writeEntry(pOutput, pLeft);
                pLeftCount--;
                pLeft = pLeftCount > IConstants.ZERO_LONG
                        ? RecordingFileFormat.readIndexEntry(pLeftInput)
                        : null;
            } else {
                writeEntry(pOutput, pRight);
                pRightCount--;
                pRight = pRightCount > IConstants.ZERO_LONG
                        ? RecordingFileFormat.readIndexEntry(pRightInput)
                        : null;
            }
        }
        while (pLeftCount > IConstants.ZERO_LONG) {
            if (pLeft == null) {
                throw new IOException(IConstants.ERR_INDEX_IO);
            }
            writeEntry(pOutput, pLeft);
            pLeftCount--;
            pLeft = pLeftCount > IConstants.ZERO_LONG
                    ? RecordingFileFormat.readIndexEntry(pLeftInput)
                    : null;
        }
        while (pRightCount > IConstants.ZERO_LONG) {
            if (pRight == null) {
                throw new IOException(IConstants.ERR_INDEX_IO);
            }
            writeEntry(pOutput, pRight);
            pRightCount--;
            pRight = pRightCount > IConstants.ZERO_LONG
                    ? RecordingFileFormat.readIndexEntry(pRightInput)
                    : null;
        }
    }

    private void writeSortedFile(Path[] pTapes, Path pSortedFile) throws IOException {
        Files.deleteIfExists(pSortedFile);
        try (RandomAccessFile pOutput = new RandomAccessFile(pSortedFile.toFile(), IConstants.FILE_MODE_RW);
                RandomAccessFile pFirstTape = new RandomAccessFile(pTapes[0].toFile(), IConstants.FILE_MODE_R);
                RandomAccessFile pSecondTape = new RandomAccessFile(pTapes[1].toFile(), IConstants.FILE_MODE_R)) {
            copyTape(pFirstTape, pOutput);
            copyTape(pSecondTape, pOutput);
        }
    }

    private void copyTape(RandomAccessFile pInput, RandomAccessFile pOutput) throws IOException {
        while (pInput.getFilePointer() < pInput.length()) {
            writeEntry(pOutput, RecordingFileFormat.readIndexEntry(pInput));
        }
    }

    private void writeEntry(RandomAccessFile pOutput, RecordingFileFormat.IndexEntry pEntry)
            throws IOException {
        RecordingFileFormat.writeIndexEntry(pOutput, pEntry.name(), pEntry.headerPosition());
    }

    private Path[] tapePaths(Path pDirectory, String[] pTapeNames) {
        Path[] pPaths = new Path[IConstants.MERGE_TAPES];
        for (int pIndex = IConstants.ZERO_INT; pIndex < IConstants.MERGE_TAPES; pIndex++) {
            pPaths[pIndex] = pDirectory.resolve(IConstants.SORT_TAPE_PREFIX + pTapeNames[pIndex]);
        }
        return pPaths;
    }

    private void truncateTapes(Path[] pTapes) throws IOException {
        for (Path pTape : pTapes) {
            try (RandomAccessFile pOutput = new RandomAccessFile(pTape.toFile(), IConstants.FILE_MODE_RW)) {
                pOutput.setLength(IConstants.ZERO_LONG);
            }
        }
    }

    private void deleteTapes(Path pDirectory) throws IOException {
        for (String pTapeName : IConstants.SORT_TAPES_A) {
            Files.deleteIfExists(pDirectory.resolve(IConstants.SORT_TAPE_PREFIX + pTapeName));
        }
        for (String pTapeName : IConstants.SORT_TAPES_B) {
            Files.deleteIfExists(pDirectory.resolve(IConstants.SORT_TAPE_PREFIX + pTapeName));
        }
    }

    private void replaceFile(Path pSource, Path pTarget) throws IOException {
        try {
            Files.move(pSource, pTarget, REPLACE_EXISTING, ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException pException) {
            Files.move(pSource, pTarget, REPLACE_EXISTING);
        }
    }
}

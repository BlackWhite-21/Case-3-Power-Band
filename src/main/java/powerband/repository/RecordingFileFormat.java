package powerband.repository;

import java.io.IOException;
import java.io.RandomAccessFile;

import powerband.dto.SynthEvent;
import powerband.util.IConstants;

final class RecordingFileFormat {

    private RecordingFileFormat() {
    }

    static void writeHeader(
            RandomAccessFile pOutput,
            String pName,
            long pStartOffset,
            long pEndOffset,
            long pRecordedAtEpochMillis) throws IOException {
        pOutput.writeByte(IConstants.RECORD_TYPE_HEADER);
        writeFixedName(pOutput, pName);
        pOutput.writeLong(pStartOffset);
        pOutput.writeLong(pEndOffset);
        pOutput.writeLong(pRecordedAtEpochMillis);
        writePadding(pOutput, IConstants.MUSIC_RECORD_SIZE - IConstants.MUSIC_HEADER_FIXED_BYTES);
    }

    static void writeCommand(RandomAccessFile pOutput, SynthEvent pEvent) throws IOException {
        pOutput.writeByte(IConstants.RECORD_TYPE_COMMAND);
        pOutput.writeLong(pEvent.offsetMs());
        pOutput.writeInt(pEvent.synthId());
        pOutput.writeInt(pEvent.action());
        pOutput.writeInt(pEvent.note());
        pOutput.writeInt(pEvent.durMs());
        pOutput.writeFloat(pEvent.velocity());
        pOutput.writeFloat(pEvent.timbre1());
        pOutput.writeFloat(pEvent.timbre2());
        pOutput.writeFloat(pEvent.timbre3());
        writePadding(pOutput, IConstants.MUSIC_RECORD_SIZE - IConstants.MUSIC_COMMAND_FIXED_BYTES);
    }

    static void writeIndexEntry(RandomAccessFile pOutput, String pName, long pHeaderPosition)
            throws IOException {
        writeFixedName(pOutput, pName);
        pOutput.writeLong(pHeaderPosition);
    }

    static HeaderEntry readHeader(RandomAccessFile pInput) throws IOException {
        byte pRecordType = pInput.readByte();
        if (pRecordType != IConstants.RECORD_TYPE_HEADER) {
            throw new IOException(IConstants.ERR_RECORD_TYPE + pRecordType);
        }
        String pName = readFixedName(pInput);
        long pStartOffset = pInput.readLong();
        long pEndOffset = pInput.readLong();
        long pRecordedAtEpochMillis = pInput.readLong();
        return new HeaderEntry(
                pName,
                pStartOffset,
                pEndOffset,
                pRecordedAtEpochMillis);
    }

    static SynthEvent readCommand(RandomAccessFile pInput) throws IOException {
        byte pRecordType = pInput.readByte();
        if (pRecordType != IConstants.RECORD_TYPE_COMMAND) {
            throw new IOException(IConstants.ERR_RECORD_TYPE + pRecordType);
        }
        long pOffsetMs = pInput.readLong();
        int pSynthId = pInput.readInt();
        int pAction = pInput.readInt();
        int pNote = pInput.readInt();
        int pDurMs = pInput.readInt();
        float pVelocity = pInput.readFloat();
        float pTimbre1 = pInput.readFloat();
        float pTimbre2 = pInput.readFloat();
        float pTimbre3 = pInput.readFloat();
        if (pOffsetMs < IConstants.ZERO_LONG) {
            throw new IOException(IConstants.ERR_RECORD_TYPE);
        }
        return new SynthEvent(
                pOffsetMs,
                pSynthId,
                pAction,
                pNote,
                pDurMs,
                pVelocity,
                pTimbre1,
                pTimbre2,
                pTimbre3);
    }

    static IndexEntry readIndexEntry(RandomAccessFile pInput) throws IOException {
        return new IndexEntry(readFixedName(pInput), pInput.readLong());
    }

    private static void writeFixedName(RandomAccessFile pOutput, String pName) throws IOException {
        if (pName.length() > IConstants.NAME_LENGTH) {
            throw new IOException(IConstants.ERR_RECORD_SIZE + IConstants.NAME_LENGTH);
        }
        for (int pIndex = IConstants.ZERO_INT; pIndex < IConstants.NAME_LENGTH; pIndex++) {
            char pCharacter = pIndex < pName.length()
                    ? pName.charAt(pIndex)
                    : IConstants.PADDING_CHAR;
            pOutput.writeChar(pCharacter);
        }
    }

    private static String readFixedName(RandomAccessFile pInput) throws IOException {
        StringBuilder pName = new StringBuilder(IConstants.NAME_LENGTH);
        boolean pPaddingStarted = false;
        for (int pIndex = IConstants.ZERO_INT; pIndex < IConstants.NAME_LENGTH; pIndex++) {
            char pCharacter = pInput.readChar();
            if (pCharacter == IConstants.PADDING_CHAR) {
                pPaddingStarted = true;
            } else if (pPaddingStarted) {
                throw new IOException(IConstants.ERR_INDEX_IO);
            } else {
                pName.append(pCharacter);
            }
        }
        return pName.toString();
    }

    private static void writePadding(RandomAccessFile pOutput, int pPaddingSize) throws IOException {
        for (int pIndex = IConstants.ZERO_INT; pIndex < pPaddingSize; pIndex++) {
            pOutput.writeByte(IConstants.PADDING_BYTE);
        }
    }

    record IndexEntry(String name, long headerPosition) {
    }

    record HeaderEntry(
            String name,
            long startOffset,
            long endOffset,
            long recordedAtEpochMillis) {
    }
}

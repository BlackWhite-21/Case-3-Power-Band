package powerband.instrument;

import static powerband.util.IConstants.BITS_PER_BYTE;
import static powerband.util.IConstants.BYTE_MASK;
import static powerband.util.IConstants.ERR_MIC_OPEN;
import static powerband.util.IConstants.ERR_MIC_READ;
import static powerband.util.IConstants.MIC_BIG_ENDIAN;
import static powerband.util.IConstants.MIC_BYTES_PER_SAMPLE;
import static powerband.util.IConstants.MIC_CHANNELS;
import static powerband.util.IConstants.MIC_FRAME_BYTES;
import static powerband.util.IConstants.MIC_FRAME_SAMPLES;
import static powerband.util.IConstants.MIC_LINE_BUFFER_FRAMES;
import static powerband.util.IConstants.MIC_SAMPLE_BITS;
import static powerband.util.IConstants.MIC_SAMPLE_FULL_SCALE;
import static powerband.util.IConstants.MIC_SAMPLE_RATE;
import static powerband.util.IConstants.MIC_SIGNED;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;

import powerband.exception.DeviceException;

/**
 * Captures mono PCM from Java Sound's default target data line.
 */
public class DefaultMicrophoneAudioSource implements MicrophoneAudioSource {

    private volatile TargetDataLine line;

    @Override
    public void open() throws DeviceException {
        AudioFormat format = new AudioFormat(MIC_SAMPLE_RATE, MIC_SAMPLE_BITS,
                MIC_CHANNELS, MIC_SIGNED, MIC_BIG_ENDIAN);
        try {
            TargetDataLine openedLine = AudioSystem.getTargetDataLine(format);
            openedLine.open(format, MIC_FRAME_BYTES * MIC_LINE_BUFFER_FRAMES);
            openedLine.start();
            line = openedLine;
        } catch (LineUnavailableException | IllegalArgumentException e) {
            throw new DeviceException(ERR_MIC_OPEN, e);
        }
    }

    @Override
    public float[] readFrame() throws DeviceException {
        TargetDataLine activeLine = line;
        if (activeLine == null || !activeLine.isOpen()) {
            throw new DeviceException(ERR_MIC_READ);
        }

        byte[] bytes = new byte[MIC_FRAME_BYTES];
        int read = 0;
        while (read < bytes.length) {
            int count = activeLine.read(bytes, read, bytes.length - read);
            if (count <= 0) {
                throw new DeviceException(ERR_MIC_READ);
            }
            read += count;
        }

        float[] samples = new float[MIC_FRAME_SAMPLES];
        for (int sampleIndex = 0; sampleIndex < samples.length; sampleIndex++) {
            int byteIndex = sampleIndex * MIC_BYTES_PER_SAMPLE;
            int firstByte = bytes[byteIndex] & BYTE_MASK;
            int secondByte = bytes[byteIndex + 1] & BYTE_MASK;
            int rawSample = MIC_BIG_ENDIAN
                    ? (firstByte << BITS_PER_BYTE) | secondByte
                    : (secondByte << BITS_PER_BYTE) | firstByte;
            samples[sampleIndex] = (short) rawSample / (float) MIC_SAMPLE_FULL_SCALE;
        }
        return samples;
    }

    @Override
    public void close() {
        TargetDataLine activeLine = line;
        line = null;
        if (activeLine != null) {
            activeLine.stop();
            activeLine.close();
        }
    }
}

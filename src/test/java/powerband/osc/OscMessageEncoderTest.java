package powerband.osc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

class OscMessageEncoderTest {

    private final OscMessageEncoder encoder = new OscMessageEncoder();

    @Test
    void messageLengthIsMultipleOfFour() throws Exception {
        byte[] data = encoder.encode("/s_new", "classicPiano", 10000, 0, 1, "note", 48, "amp", 0.3f);
        assertEquals(0, data.length % 4);
    }

    @Test
    void encodesPathTypeTagsAndBigEndianInt() throws Exception {
        byte[] data = encoder.encode("/a", 7);
        // "/a" + 2 ceros = 4 bytes, ",i" + 2 ceros = 4 bytes, int = 4 bytes
        assertEquals(12, data.length);
        assertEquals("/a", new String(data, 0, 2, StandardCharsets.US_ASCII));
        assertEquals(",i", new String(data, 4, 2, StandardCharsets.US_ASCII));
        assertEquals(7, ByteBuffer.wrap(data, 8, 4).getInt());
    }

    @Test
    void encodesFloatAsIeeeBits() throws Exception {
        byte[] data = encoder.encode("/a", 0.5f);
        assertTrue(new String(data, StandardCharsets.US_ASCII).contains(",f"));
        assertEquals(0.5f, ByteBuffer.wrap(data, 8, 4).getFloat());
    }

    @Test
    void rejectsUnsupportedType() {
        assertThrows(IllegalArgumentException.class, () -> encoder.encode("/a", 1.0d));
    }
}

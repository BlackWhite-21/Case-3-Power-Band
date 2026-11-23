package powerband.osc;

import static powerband.util.IConstants.ERR_OSC_TYPE;
import static powerband.util.IConstants.OSC_ALIGNMENT;
import static powerband.util.IConstants.OSC_TYPE_FLOAT;
import static powerband.util.IConstants.OSC_TYPE_INT;
import static powerband.util.IConstants.OSC_TYPE_STRING;
import static powerband.util.IConstants.OSC_TYPE_TAG_START;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Convierte un mensaje OSC (ruta + argumentos) en bytes.
 * Formato OSC: ruta, etiqueta de tipos (",ifs"), argumentos. Todo alineado a 4 bytes, big-endian.
 */
public class OscMessageEncoder {

    public byte[] encode(String pPath, Object... pArgs) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(bytes)) {
            writePaddedString(out, pPath);
            writePaddedString(out, buildTypeTags(pArgs));
            for (Object arg : pArgs) {
                writeArgument(out, arg);
            }
        }
        return bytes.toByteArray();
    }

    private String buildTypeTags(Object[] pArgs) {
        StringBuilder tags = new StringBuilder().append(OSC_TYPE_TAG_START);
        for (Object arg : pArgs) {
            tags.append(typeTagOf(arg));
        }
        return tags.toString();
    }

    private char typeTagOf(Object pArg) {
        if (pArg instanceof Integer) {
            return OSC_TYPE_INT;
        }
        if (pArg instanceof Float) {
            return OSC_TYPE_FLOAT;
        }
        if (pArg instanceof String) {
            return OSC_TYPE_STRING;
        }
        throw new IllegalArgumentException(ERR_OSC_TYPE + pArg);
    }

    private void writeArgument(DataOutputStream pOut, Object pArg) throws IOException {
        switch (pArg) {
            case Integer value -> pOut.writeInt(value);
            case Float value -> pOut.writeFloat(value);
            case String value -> writePaddedString(pOut, value);
            default -> throw new IllegalArgumentException(ERR_OSC_TYPE + pArg);
        }
    }

    /** Texto ASCII + al menos un byte 0, rellenado hasta múltiplo de 4. */
    private void writePaddedString(DataOutputStream pOut, String pText) throws IOException {
        byte[] text = pText.getBytes(StandardCharsets.US_ASCII);
        pOut.write(text);
        int padding = OSC_ALIGNMENT - (text.length % OSC_ALIGNMENT);
        for (int i = 0; i < padding; i++) {
            pOut.write(0);
        }
    }
}

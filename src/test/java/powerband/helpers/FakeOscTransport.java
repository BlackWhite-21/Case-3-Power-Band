package powerband.helpers;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import powerband.osc.IOscTransport;

/**
 * Transporte OSC falso: guarda los paquetes enviados en vez de mandarlos por UDP.
 */
public class FakeOscTransport implements IOscTransport {

    private final List<byte[]> sent = new ArrayList<>();
    private byte[] reply;

    @Override
    public void send(byte[] pData) {
        sent.add(pData);
    }

    @Override
    public byte[] receive(int pTimeoutMs) throws IOException {
        if (reply == null) {
            throw new SocketTimeoutException();
        }
        return reply;
    }

    public void setReply(String pReply) {
        this.reply = pReply.getBytes(StandardCharsets.US_ASCII);
    }

    @Override
    public void close() {
    }

    public List<byte[]> getSent() {
        return sent;
    }

    /** Texto ASCII del último paquete (para buscar ruta y nombres dentro). */
    public String lastAsText() {
        return new String(sent.get(sent.size() - 1), StandardCharsets.US_ASCII);
    }
}

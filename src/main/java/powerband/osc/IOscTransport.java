package powerband.osc;

import java.io.IOException;

/**
 * Canal por donde viajan los bytes OSC. Permite cambiar UDP por un falso en las pruebas.
 */
public interface IOscTransport extends AutoCloseable {

    void send(byte[] pData) throws IOException;

    /** Espera un paquete de respuesta; lanza SocketTimeoutException si no llega a tiempo. */
    byte[] receive(int pTimeoutMs) throws IOException;

    @Override
    void close();
}

package powerband.osc;

import static powerband.util.IConstants.OSC_RECEIVE_BUFFER;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Arrays;

/**
 * Envía paquetes OSC por UDP al servidor scsynth de SuperCollider.
 */
public class UdpOscTransport implements IOscTransport {

    private final DatagramSocket socket;
    private final InetAddress address;
    private final int port;

    public UdpOscTransport(String pHost, int pPort) throws IOException {
        this.socket = new DatagramSocket();
        this.address = InetAddress.getByName(pHost);
        this.port = pPort;
    }

    @Override
    public void send(byte[] pData) throws IOException {
        socket.send(new DatagramPacket(pData, pData.length, address, port));
    }

    @Override
    public byte[] receive(int pTimeoutMs) throws IOException {
        byte[] buffer = new byte[OSC_RECEIVE_BUFFER];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        socket.setSoTimeout(pTimeoutMs);
        socket.receive(packet);
        return Arrays.copyOf(packet.getData(), packet.getLength());
    }

    @Override
    public void close() {
        socket.close();
    }
}

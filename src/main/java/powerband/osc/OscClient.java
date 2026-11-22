package powerband.osc;

import static powerband.util.IConstants.ERR_SC_SEND;
import static powerband.util.IConstants.ERR_SC_TIMEOUT;
import static powerband.util.IConstants.OSC_ADD_TO_HEAD;
import static powerband.util.IConstants.OSC_DEFAULT_GROUP;
import static powerband.util.IConstants.OSC_FIRST_NODE_ID;
import static powerband.util.IConstants.OSC_NODE_SET;
import static powerband.util.IConstants.OSC_SNEW_FIXED_ARGS;
import static powerband.util.IConstants.OSC_STATUS;
import static powerband.util.IConstants.OSC_STATUS_REPLY;
import static powerband.util.IConstants.OSC_SYNTH_NEW;
import static powerband.util.IConstants.SC_PING_TIMEOUT_MS;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

import powerband.exception.SynthException;

/**
 * Cliente OSC de scsynth (basado en el BandClient del profesor).
 * Crea synths con /s_new y cambia sus controles con /n_set.
 */
public class OscClient implements AutoCloseable {

    private final IOscTransport transport;
    private final OscMessageEncoder encoder;
    private final AtomicInteger nextNodeId;

    public OscClient(IOscTransport pTransport) {
        this.transport = pTransport;
        this.encoder = new OscMessageEncoder();
        this.nextNodeId = new AtomicInteger(OSC_FIRST_NODE_ID);
    }

    /** Verifica que scsynth esté escuchando: manda /status y espera /status.reply. */
    public void ping() throws SynthException {
        try {
            send(OSC_STATUS);
            byte[] reply = transport.receive(SC_PING_TIMEOUT_MS);
            String text = new String(reply, StandardCharsets.US_ASCII);
            if (!text.startsWith(OSC_STATUS_REPLY)) {
                throw new SynthException(ERR_SC_TIMEOUT);
            }
        } catch (IOException e) {
            throw new SynthException(ERR_SC_TIMEOUT, e);
        }
    }

    /**
     * /s_new defName nodeId addAction target [nombre valor ...]
     * @return id del nodo creado (sirve para apagarlo después)
     */
    public int newSynth(String pDefName, Object... pControls) throws SynthException {
        int nodeId = nextNodeId.getAndIncrement();
        Object[] args = new Object[pControls.length + OSC_SNEW_FIXED_ARGS];
        args[0] = pDefName;
        args[1] = nodeId;
        args[2] = OSC_ADD_TO_HEAD;
        args[3] = OSC_DEFAULT_GROUP;
        System.arraycopy(pControls, 0, args, OSC_SNEW_FIXED_ARGS, pControls.length);
        send(OSC_SYNTH_NEW, args);
        return nodeId;
    }

    /** /n_set nodeId nombre valor */
    public void setControl(int pNodeId, String pName, int pValue) throws SynthException {
        send(OSC_NODE_SET, pNodeId, pName, pValue);
    }

    public void setControl(int pNodeId, String pName, float pValue) throws SynthException {
        send(OSC_NODE_SET, pNodeId, pName, pValue);
    }

    /** synchronized: varios instrumentos (hilos) comparten el mismo socket. */
    private synchronized void send(String pPath, Object... pArgs) throws SynthException {
        try {
            transport.send(encoder.encode(pPath, pArgs));
        } catch (IOException e) {
            throw new SynthException(ERR_SC_SEND, e);
        }
    }

    @Override
    public void close() {
        transport.close();
    }
}

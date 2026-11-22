package powerband.synth;

import static powerband.util.IConstants.ERR_UNKNOWN_SYNTH;

import java.util.LinkedHashMap;
import java.util.Map;

import powerband.dto.SynthEvent;
import powerband.exception.SynthException;

/**
 * Guarda un SynthCommand por id de synth. El reproductor lo usa para saber a qué synth
 * mandar cada comando leído del disco.
 */
public class SynthCommandRegistry {

    private final Map<Integer, SynthCommand> commands;

    public SynthCommandRegistry() {
        this.commands = new LinkedHashMap<>();
    }

    public void register(SynthCommand pCommand) {
        commands.put(pCommand.getSynthId(), pCommand);
    }

    public SynthCommand get(int pSynthId) throws SynthException {
        SynthCommand command = commands.get(pSynthId);
        if (command == null) {
            throw new SynthException(ERR_UNKNOWN_SYNTH + pSynthId);
        }
        return command;
    }

    public void execute(SynthEvent pEvent) throws SynthException {
        get(pEvent.synthId()).execute(pEvent);
    }

    public String nameOf(int pSynthId) {
        SynthCommand command = commands.get(pSynthId);
        return command == null ? String.valueOf(pSynthId) : command.getSynthName();
    }

    public void stopAll() {
        for (SynthCommand command : commands.values()) {
            try {
                command.stopAll();
            } catch (SynthException e) {
                System.err.println(e.getMessage());
            }
        }
    }
}

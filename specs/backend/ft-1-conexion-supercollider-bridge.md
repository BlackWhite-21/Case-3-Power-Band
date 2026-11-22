---
id: ft-1
layer: backend
status: approved
---

# Conexión con SuperCollider y comandos de los synths (Bridge - implementación)

## General description

- Cliente OSC sobre UDP hacia scsynth (puerto 57110), basado en `BandClient.java` del profesor.
- Jerarquía `SynthCommand` = implementor del Bridge: un comando por synth de `myband.sc` (`classicPiano`, `stringBass`, `organ70s`).
- Cada comando traduce un `SynthEvent` genérico (nota, duración, velocidad, 3 timbres 0..1) a los parámetros de su synth.

## Inputs

- `SynthEvent` (dto): offsetMs, synthId, action (PLAY/ON/OFF), note, durMs, velocity, timbre1..3.
- Host y puerto de scsynth (argumentos de `main` o constantes).

## Outputs

- Paquetes OSC `/s_new defName nodeId 0 1 [param valor...]` y `/n_set nodeId gate 0`.
- `/status` para verificar que el servidor responde (`/status.reply`).

## Test criteria

- Todo mensaje OSC mide un múltiplo de 4 bytes; enteros y floats big-endian.
- Piano manda `/s_new` con `classicPiano` y `third` (4 mayor si timbre1 >= 0.5, si no 3).
- Bajo manda `/s_new` con `stringBass`.
- Órgano: ON manda `/s_new organ70s`; OFF manda `/n_set ... gate 0`; OFF sin ON no manda nada.
- Piano y bajo ignoran OFF (se apagan solos).
- `SynthCommandRegistry` envía cada evento al comando de su synthId; id desconocido lanza `SynthException`.
- `ping()` lanza `SynthException` si no hay respuesta.

## Assets

- `supercollider/myband.sc` (synths del profesor, sin cambios).
- Constantes OSC_*, SYNTH_*, PARAM_*, PIANO_*, BASS_*, ORGAN_* en `IConstants`.
- Diagrama PlantUML `specs/diagrams/ft-1-bridge-synths.puml` (imagen en `specs/diagrams/png/ft-1-bridge-synths.png`):

```plantuml
@startuml ft-1-bridge-synths
title ft-1 - Comandos de synths (implementor del Bridge) y OSC
skinparam classAttributeIconSize 0
hide empty members

abstract class SynthCommand <<Implementor>> {
  - client : OscClient
  + {abstract} getSynthId() : int
  + {abstract} getSynthName() : String
  + execute(pEvent : SynthEvent)
  # {abstract} play(pEvent : SynthEvent)
  # start(pEvent : SynthEvent)
  # stop(pEvent : SynthEvent)
  + stopAll()
  # scale(pValue, pMin, pMax) : float
  # midiToFreq(pNote) : float
}
note right of SynthCommand::execute
  Método plantilla:
  ON  -> start()
  OFF -> stop()
  PLAY -> play()
end note

class PianoCommand {
  # play() : /s_new classicPiano note durMs third amp
}
class BassCommand {
  # play() : /s_new stringBass note durMs amp
}
class OrganCommand {
  - activeNodes : Map<Integer,Integer>
  - releaser : ScheduledExecutorService
  # start() : /s_new organ70s freq r1..r3 a1..a3
  # stop() : /n_set nodeId gate 0
  # play() : start + apagado programado
  + stopAll()
}
class SynthCommandRegistry {
  - commands : Map<Integer, SynthCommand>
  + register(pCommand)
  + get(pSynthId) : SynthCommand
  + execute(pEvent)
  + nameOf(pSynthId) : String
  + stopAll()
}
class OscClient {
  - transport : IOscTransport
  - encoder : OscMessageEncoder
  - nextNodeId : AtomicInteger
  + ping()
  + newSynth(pDefName, pControls...) : int
  + setControl(pNodeId, pName, pValue)
  - send(pPath, pArgs...) <<synchronized>>
}
class OscMessageEncoder {
  + encode(pPath, pArgs...) : byte[]
}
interface IOscTransport {
  + send(pData : byte[])
  + receive(pTimeoutMs) : byte[]
  + close()
}
class UdpOscTransport {
  - socket : DatagramSocket
}
class SynthException

SynthCommand <|-- PianoCommand
SynthCommand <|-- BassCommand
SynthCommand <|-- OrganCommand
SynthCommandRegistry o--> SynthCommand
SynthCommand --> OscClient
OscClient --> OscMessageEncoder
OscClient --> IOscTransport
IOscTransport <|.. UdpOscTransport
OscClient ..> SynthException : lanza

@enduml
```

- Diagrama PlantUML `specs/diagrams/ft-1-envio-comando.puml` (imagen en `specs/diagrams/png/ft-1-envio-comando.png`):

```plantuml
@startuml ft-1-envio-comando
title ft-1 - Envío de un comando a SuperCollider
participant ":PianoCommand" as P
participant ":OscClient" as C
participant ":OscMessageEncoder" as E
participant ":UdpOscTransport" as T
participant "scsynth\n(UDP 57110)" as SC

[-> P : execute(evento PLAY)
P -> P : play(evento)
P -> C : newSynth("classicPiano", note, durMs, third, amp)
C -> C : nodeId = nextNodeId++
C -> E : encode("/s_new", args)
E --> C : byte[] (alineado a 4)
C -> T : send(bytes)
T -> SC : DatagramPacket
SC -> SC : crea Synth(\\classicPiano)
C --> P : nodeId
@enduml
```

## Implementation guidelines

- Paquetes: `src/main/java/powerband/osc/` (`IOscTransport`, `UdpOscTransport`, `OscMessageEncoder`, `OscClient`) y `src/main/java/powerband/synth/` (`SynthCommand`, `PianoCommand`, `BassCommand`, `OrganCommand`, `SynthCommandRegistry`).
- `SynthCommand.execute` es método plantilla: ON → start, OFF → stop, PLAY → play. Defaults: start = play, stop = nada.
- `OscClient.send` es synchronized (3 hilos de instrumentos comparten el socket).
- `OrganCommand` guarda nota → nodeId y programa el apagado cuando recibe PLAY (otro instrumento conectado por Bridge).
- Errores de `IOException` se envuelven en `SynthException`.

## Business workflow

- Main crea `OscClient(new UdpOscTransport(host, port))` y llama `ping()`; si falla avisa y sigue.
- Main registra los tres comandos en `SynthCommandRegistry`.
- Al recibir un evento, el comando lo convierte a OSC y lo envía.

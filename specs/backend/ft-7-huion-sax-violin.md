---
id: ft-7
layer: backend
status: approved
---

# Tableta Huion, saxofón y violín

## General description

- Agregar los synths `saxophone` y `violin` en SuperCollider y sus comandos Java.
- Conectar la tableta Huion al saxofón: X selecciona la nota, Y la octava y la presión controla el volumen y el estado de la nota.
- Conectar el micrófono predeterminado del sistema al violín. El joystick y `organ70s` no cambian.
- Retirar la captura de cámara y sus tipos y constantes específicos. Mantener `classicPiano` y `stringBass` como synths legacy para reproducir grabaciones existentes.

## Inputs

- Lectura normalizada de tableta: `x`, `y`, `pressure` en `[0,1]`; presión cero representa lápiz levantado o sin contacto.
- Audio PCM del dispositivo de entrada predeterminado, capturado con `javax.sound.sampled`.
- Eventos de micrófono y tableta convertidos a `SynthEvent` (`synthId`, acción, nota MIDI y velocidad).

## Outputs

- Eventos Huion al `SaxophoneCommand`; eventos de micrófono al `ViolinCommand`; eventos del joystick continúan al `OrganCommand`.
- Mensajes OSC `/s_new` y `/n_set` para `saxophone` y `violin`.
- Registro de comandos con IDs estables: piano `1`, bajo `2`, órgano `3`, saxofón `4` y violín `5`. No cambiar el formato ni el tamaño de los registros existentes.

## Test criteria

- La zona activa de la tableta se normaliza y limita a `[0,1]`. X cuantiza a los 12 semitonos de una octava; Y elige la octava configurada. El MIDI resultante se limita a `[0,127]`.
- Los valores por defecto conservan el rango previo de `48` a `71` (C3 a B4), definido en `IConstants`, no como valores quemados.
- Presión positiva inicia una nota con volumen proporcional a la presión. Cambiar la presión de una nota sostenida actualiza `amp` sin volver a dispararla.
- Si X/Y seleccionan otra nota mientras el lápiz sigue apoyado, se apaga la nota anterior y se inicia la nueva. Al levantar el lápiz se envía un único OFF; presión cero nunca deja una nota sonando.
- `SaxophoneCommand` y `ViolinCommand` traducen PLAY/ON/OFF a OSC; los ON/OFF usan `gate`, y PLAY termina tras la duración del evento.
- El micrófono usa la entrada predeterminada, convierte el pitch detectado a nota MIDI y el RMS a volumen; inicio y fin de sonido generan ON/OFF para el violín.
- El registro conserva y reproduce eventos históricos de piano y bajo usando sus synths legacy; los IDs de órgano no cambian.
- `myband.sc` carga `classicPiano`, `stringBass`, `organ70s`, `saxophone` y `violin`; los tres dispositivos activos solo se asignan a órgano, saxofón y violín.
- No quedan lector, DTO, constantes, errores ni referencias de implementación que requieran cámara u OpenCV.

## Assets

- Entrada física Huion detrás de una interfaz/adaptador que entregue coordenadas y presión normalizadas. El modelo y API/controlador Huion concretos no están definidos; mantenerlos aislados y no depender de emulación de mouse, que no proporciona la presión requerida.
- Comandos OSC de los synths y parámetros exactos en `supercollider/myband.sc`.
- Diagrama PlantUML `../diagrams/ft-7-huion-sax-violin.puml`:

```plantuml
@startuml ft-7-huion-sax-violin
title ft-7 - Instrumentos Huion y micrófono con synths
skinparam classAttributeIconSize 0
hide empty members

record TabletReading {
  x : float
  y : float
  pressure : float
}

interface HuionInputSource {
  +read() : TabletReading
}

abstract class Instrument <<Abstraction>> {
  - command : SynthCommand
  + start()
  + stop()
}

class HuionInstrument {
  - mapper : HuionInputMapper
  + accept(pReading : TabletReading)
}

class MicrophoneInstrument {
  - input : TargetDataLine
  + start()
  + stop()
}

class HuionInputMapper {
  + map(pReading : TabletReading) : List<SynthEvent>
}

abstract class SynthCommand <<Implementor>> {
  + execute(pEvent : SynthEvent)
}

class SaxophoneCommand {
  - activeNode : int
  + execute(pEvent : SynthEvent)
}

class ViolinCommand {
  - activeNode : int
  + execute(pEvent : SynthEvent)
}

class OrganCommand
class OscClient {
  + newSynth(pName, pControls...) : int
  + setControl(pNodeId, pName, pValue)
}
class SynthEvent

HuionInputSource --> HuionInstrument
HuionInstrument --|> Instrument
MicrophoneInstrument --|> Instrument
HuionInstrument --> HuionInputMapper
HuionInputMapper --> TabletReading
HuionInputMapper ..> SynthEvent
Instrument o--> SynthCommand
SynthCommand <|-- SaxophoneCommand
SynthCommand <|-- ViolinCommand
SynthCommand <|-- OrganCommand
SaxophoneCommand --> OscClient
ViolinCommand --> OscClient
OrganCommand --> OscClient
@enduml
```

## Implementation guidelines

- Java 25+, Maven y librería estándar; no agregar OpenCV ni otra dependencia salvo que la API del hardware lo requiera y se justifique.
- Implementar instrumentos en `src/main/java/powerband/instrument/`, DTOs en `src/main/java/powerband/dto/`, comandos en `src/main/java/powerband/synth/`, constantes en `src/main/java/powerband/util/IConstants.java` y errores propios en `src/main/java/powerband/exception/`.
- Actualizar `src/main/java/powerband/osc/OscClient.java` para enviar valores float con `/n_set`, de forma que la presión pueda variar el volumen de una nota sostenida.
- Asignar IDs nuevos al saxofón y al violín; preservar los IDs de piano, bajo y órgano porque los recordings los almacenan.
- En `src/main/java/powerband/dto/`, reemplazar `MotionReading` por una lectura específica de la tableta. Eliminar constantes `CAM_*`, errores de cámara y cualquier lector/clase de cámara.
- Actualizar `supercollider/myband.sc`, `readme.md`, `CLAUDE.md` y `.github/copilot-instructions.md` para documentar los tres dispositivos activos y el soporte legacy de piano/bajo.
- No alterar el registro binario fijo ni sus tiempos; mantener cerrado cualquier stream con try-with-resources y reportar errores de dispositivo con excepciones propias.

## Business workflow

- Abrir el adaptador Huion y obtener lecturas normalizadas; si el dispositivo no está disponible, reportarlo sin impedir iniciar los otros instrumentos.
- Convertir X/Y a nota MIDI y octava, y presión a volumen.
- Emitir ON al comenzar contacto, actualizar el volumen mientras se mantiene la misma nota, cambiar de nota con OFF/ON y emitir OFF al levantar el lápiz.
- Capturar PCM del micrófono predeterminado, estimar pitch y nivel, y emitir ON/OFF al `ViolinCommand`.
- Traducir eventos a OSC y publicar los mismos eventos al editor/grabador.
- Al cargar grabaciones antiguas, resolver los IDs 1 y 2 a los synths legacy sin alterar su formato.

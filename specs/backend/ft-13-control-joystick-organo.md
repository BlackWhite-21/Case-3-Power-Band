---
id: ft-13
layer: backend
status: approved
---

# Control del órgano con joystick

## General description

- Implementar el joystick USB como instrumento activo mediante WinMM.
- Conectar `JoystickInstrument` a `OrganCommand` usando el patrón Bridge.
- Leer botones y ejes de forma periódica y transformar cada lectura en `SynthEvent`.

## Inputs

- Estado `JOYINFOEX` de `winmm.dll`: eje X, eje Y, eje Z y máscara de botones.
- Hardware disponible únicamente en Windows x64.
- `SynthEvent` publicados al editor/grabador.

## Outputs

- `JoystickState` normalizado con tres ejes `[0,1]` y máscara de botones.
- `SynthEvent` ON/OFF y actualizaciones del órgano.
- `DeviceException` ante OS no compatible, ausencia o falla de lectura.

## Test criteria

- El lector usa `winmm.dll` solo en Windows y no abre teclado o mouse.
- Los tres ejes se normalizan con `JOY_AXIS_MAX` y quedan limitados a `[0,1]`.
- Cada botón se convierte a bit individual y `isPressed(pButtonIndex)` funciona con índice válido.
- Presionar un botón genera ON una vez y soltarlo genera OFF una vez.
- X/Y/Z producen los parciales y controles esperados de `OrganCommand`.
- Cambiar ejes actualiza la nota o controles sin retrigger innecesario.
- Desconexión o lectura fallida libera todas las notas activas.

## Assets

```plantuml
@startuml ft-13-control-joystick-organo
title ft-13 - Joystick USB Bridge to organ70s
hide empty members

record JoystickState {
  x : float
  y : float
  z : float
  buttons : int
  +isPressed(pButtonIndex : int) : boolean
}

abstract class Instrument {
  - command : SynthCommand
  - eventListener : Consumer<SynthEvent>
  +start()
  +stop()
}

interface JoystickInputSource {
  +open()
  +readState() : JoystickState
  +close()
}

class WinmmJoystickInputSource {
  +open()
  +readState() : JoystickState
  +close()
}

class JoystickInstrument {
  - activeButtons : int
  +accept(pState : JoystickState)
}

abstract class SynthCommand
class OrganCommand

Instrument <|-- JoystickInstrument
JoystickInputSource <|.. WinmmJoystickInputSource
JoystickInstrument --> JoystickInputSource
JoystickInstrument --> JoystickState
Instrument o--> SynthCommand
SynthCommand <|-- OrganCommand
@enduml
```

## Implementation guidelines

- Java 25+, Maven y API Foreign Function & Memory o JNA ya usada por el proyecto.
- Crear o completar clases en `src/main/java/powerband/instrument/`.
- Conservar `JoystickState` en `src/main/java/powerband/dto/`.
- Conectar `JoystickInstrument` a `OrganCommand`; no modificar `SynthCommandRegistry` salvo registro en `Main`.
- Usar `IConstants.JOY_*`, `ERR_JOY_*` y parámetros de órgano existentes.
- Ejecutar el polling fuera del EDT y liberar recursos al detener o cerrar.

## Business workflow

1. Detectar plataforma y dispositivo `winmm`.
2. Abrir el joystick y iniciar el hilo de lectura.
3. Leer `JOYINFOEX`, normalizar ejes y botones.
4. Comparar estados para detectar cambios de botón.
5. Emitir ON/OFF y actualizar controles del órgano.
6. Al desconectar o detener, liberar notas y cerrar recursos.

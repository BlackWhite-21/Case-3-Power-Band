---
id: ft-13
layer: testing
status: approved
---

# Pruebas del joystick y órgano

## General description

- Verificar la fuente falsa, el mapeo de botones/ejes y el Bridge al órgano sin hardware WinMM.

## Inputs

- `JoystickState` sintéticos con bordes y valores interiores.
- Fuente de joystick falsa y `FakeOscTransport`.

## Outputs

- Eventos `SynthEvent` y paquetes OSC capturados.
- Resultado JUnit 5.

## Test criteria

- Ejes 0, punto medio y máximo se normalizan a límites esperados.
- Valores fuera de rango se limitan a `[0,1]`.
- Botones se convierten a máscara y detectar presionado/liberado.
- Presionar y soltar genera exactamente ON/OFF.
- Cambio de eje actualiza parciales sin crear un segundo ON.
- Falla de fuente libera la nota activa y reporta `DeviceException`.
- En OS no Windows no se intenta cargar `winmm.dll`.
- `mvn test` no requiere joystick físico ni SuperCollider.

## Assets

- Diagrama: `../diagrams/ft-13-control-joystick-organo.puml`.
- Reusar `FakeOscTransport`.

## Implementation guidelines

- JUnit 5 en `src/test/java/powerband/instrument/` y `src/test/java/powerband/synth/`.
- Inyectar una implementación falsa de `JoystickInputSource`.
- Probar `OrganCommand` con el transport fake.
- Ejecutar `mvn test`.

## Business workflow

1. Crear estados sintéticos con bordes y cambios de botón.
2. Ejecutar el instrumento e capturar eventos.
3. Ejecutar `OrganCommand` sobre el transporte falso.
4. Simular desconexión y comprobar OFF/liberación.
5. Ejecutar toda la suite sin hardware.

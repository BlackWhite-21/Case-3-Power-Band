---
id: ft-2
layer: testing
status: approved
---

# Pruebas de OSC y comandos de synths

## General description

- Pruebas unitarias sin SuperCollider, con `FakeOscTransport`.

## Inputs

- `SynthEvent` de prueba.

## Outputs

- Bytes capturados por el transporte falso.

## Test criteria

- `OscMessageEncoderTest` y `SynthCommandTest` pasan con `mvn test`.

## Assets

- Diagrama de referencia: `specs/diagrams/ft-1-bridge-synths.puml` (imagen en `specs/diagrams/png/ft-1-bridge-synths.png`).
- Diagrama de referencia: `specs/diagrams/ft-1-envio-comando.puml` (imagen en `specs/diagrams/png/ft-1-envio-comando.png`).

## Implementation guidelines

- `src/test/java/powerband/helpers/FakeOscTransport.java`
- `src/test/java/powerband/osc/OscMessageEncoderTest.java`
- `src/test/java/powerband/synth/SynthCommandTest.java`

## Business workflow

- Crear transporte falso → ejecutar comando → revisar el último paquete.

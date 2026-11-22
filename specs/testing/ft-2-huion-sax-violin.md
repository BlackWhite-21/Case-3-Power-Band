---
id: ft-2
layer: testing
status: approved
---

# Pruebas de Huion, saxofón y violín

## General description

- Validar el mapeo de tableta, los comandos OSC nuevos, la selección del micrófono predeterminado y la compatibilidad legacy sin requerir hardware ni SuperCollider.

## Inputs

- Lecturas Huion sintéticas: bordes y valores interiores de X, Y y presión.
- `SynthEvent` para saxofón, violín, órgano, piano y bajo.
- Fuente de audio falsa con muestras controladas.
- `FakeOscTransport`.

## Outputs

- Eventos de nota y bytes OSC capturados por los fakes.
- Resultado de las pruebas de Maven.

## Test criteria

- X/Y producen la nota MIDI esperada en las esquinas de la zona activa, en los límites entre semitonos/octavas y para valores fuera de rango.
- Presión cero no dispara una nota; presión positiva produce ON y volumen escalado. Modificar presión en la misma nota actualiza `amp` sin crear un segundo node. Cambiar nota o levantar el lápiz apaga el node correspondiente exactamente una vez.
- `SaxophoneCommand` y `ViolinCommand` envían el nombre y parámetros OSC correctos; ON crea un node, OFF envía gate cero y OFF sin nota activa no envía paquetes.
- El lector de micrófono usa la fuente predeterminada mediante un proveedor sustituible en pruebas; pitch y RMS generan las notas y velocidades esperadas, con ON/OFF al cruzar umbrales.
- El registro enruta IDs 4/5 a saxofón/violín y preserva IDs 1/2/3 para piano/bajo/órgano.
- `mvn test` no intenta abrir la tableta, cámara, micrófono real ni servidor SuperCollider.
- La validación de limpieza confirma que no quedan `MotionReading`, clases/adaptadores de cámara, constantes `CAM_*`, errores `ERR_CAM_*` ni dependencia OpenCV en el código activo.

## Assets

- Diagrama de clases de referencia: `../diagrams/ft-2-huion-sax-violin.puml`.

## Implementation guidelines

- JUnit 5; pruebas en `src/test/java/powerband/` siguiendo el paquete de la clase probada.
- Reutilizar `src/test/java/powerband/helpers/FakeOscTransport.java`; inyectar proveedores falsos para Huion y audio.
- Actualizar `src/test/java/powerband/synth/SynthCommandTest.java`; añadir `HuionInputMapperTest` y pruebas del instrumento de micrófono.
- Ejecutar `mvn test`. No corregir fallos no relacionados con esta feature.

## Business workflow

- Inyectar lecturas/tableta o audio falsos.
- Ejecutar mapper o instrumento y capturar eventos.
- Ejecutar el comando correspondiente sobre `FakeOscTransport`.
- Verificar nota, acción, volumen, synth ID y parámetros OSC.

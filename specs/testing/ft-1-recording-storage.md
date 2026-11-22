---
id: ft-1
layer: testing
status: draft
---

# Pruebas del guardado de grabaciones

## General description

- Verifica la captura de eventos, validación de nombres y persistencia binaria sin hardware ni servidor SuperCollider.
- Comprueba el tamaño fijo, el índice ordenado en disco y la lista cronológica.

## Inputs

- Eventos sintéticos `SynthEvent`, nombres válidos e inválidos y errores de E/S simulados.
- Directorio temporal por prueba para los tres archivos de grabaciones.

## Outputs

- Pruebas JUnit 5 que aceptan o rechazan el comportamiento observable del servicio y repositorio.

## Test criteria

- Cubrir inicio, eventos con offsets relativos, finalización, rechazo de duplicados y reintento con nombre único.
- Verificar rechazo de nombres vacíos, largos o con caracteres no permitidos.
- Verificar búsqueda binaria de nombre existente y `RecordingNotFoundException` para nombre ausente.
- Verificar tamaños exactos de registros, encabezado con límites correctos, parámetros de eventos y lectura tras cerrar/reabrir archivos.
- Verificar que el índice queda ordenado después de Balanced Merge Sort y que la lista conserva el orden de grabación.
- Verificar que una falla de E/S produce excepción personalizada y que no se notifica éxito.
- No requerir dispositivos físicos, SuperCollider ni archivos compartidos entre pruebas.

## Assets

- Diagrama del flujo: `specs/diagrams/ft-1-recording-storage.puml`.
- Convenciones de pruebas: JUnit 5, clases `*Test`, espejo de paquetes bajo `src/test/java/powerband/` y `@TempDir`.

## Implementation guidelines

- Crear pruebas en `src/test/java/powerband/service/` y `src/test/java/powerband/repository/`, reflejando los paquetes productivos.
- Seguir `.github/instructions/testing-junit.instructions.md`; usar JUnit 5 y `@TempDir`.
- Probar en Windows y mantener las pruebas independientes de `winmm`, micrófono, cámara, OpenCV y scsynth.
- Ejecutar las pruebas con `mvn test`.

## Business workflow

1. Preparar archivos vacíos bajo un directorio temporal.
2. Iniciar una captura con eventos representativos de varios synths.
3. Detenerla, probar nombres inválidos y duplicados, y completar con un nombre único.
4. Reabrir los archivos y comprobar offsets, payload, tamaños, búsqueda binaria, orden del índice y orden de la lista.
5. Inyectar errores de E/S y comprobar la excepción y ausencia de un resultado exitoso.

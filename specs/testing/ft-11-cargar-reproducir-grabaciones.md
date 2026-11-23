---
id: ft-11
layer: testing
status: approved
---

# Pruebas de carga y reproducción

## General description

- Verificar listado cronológico, búsqueda por nombre, lectura binaria y reproducción temporal sin hardware ni SuperCollider.

## Inputs

- Directorio temporal con grabaciones binarias válidas.
- Archivos corruptos o truncados simulados.
- `SynthEvent` sintéticos y transportes OSC falsos.

## Outputs

- Resultado JUnit 5 de todas las operaciones de carga y reproducción.

## Test criteria

- La lista conserva el orden de grabación y excluye encabezados o líneas inválidas según la excepción definida.
- La búsqueda binaria encuentra cada nombre y rechaza nombres inexistentes.
- La carga valida tipo de encabezado, nombre, posición inicial y fin exclusivo.
- La lectura no cruza el límite final y conserva offsets, synth, acción y parámetros.
- El reproductor respeta diferencias entre offsets consecutivos y no reproduce los comandos antes de su intervalo.
- `stopPlayback()` impide ejecución posterior y termina sin liberar grabaciones.
- Los eventos legacy piano/bajo se mantienen compatibles y no cambian el formato binario.
- Los errores de archivo, encabezado o índice producen excepciones propias sin éxito parcial.
- Las pruebas no requieren devices, cámara, micrófono, tableta, joystick ni SuperCollider real.

## Assets

- Secuencia compartida: `../diagrams/ft-11-cargar-reproducir-grabaciones.puml`.
- Especificaciones relacionadas: `../frontend/ft-11-cargar-reproducir-grabaciones.md`, `../backend/ft-11-cargar-reproducir-grabaciones.md` y `../database/ft-11-cargar-reproducir-grabaciones.md`.

## Implementation guidelines

- JUnit 5 en `src/test/java/powerband/repository/`, `src/test/java/powerband/service/` y `src/test/java/powerband/ui/`.
- Usar `@TempDir`, repositorios falsos y `FakeOscTransport`.
- Inyectar un reloj o control de espera reproducible para comprobar la fidelidad temporal sin sleeps largos.
- Ejecutar `mvn test`.

## Business workflow

1. Crear grabaciones temporales con varios eventos y offsets.
2. Leer la lista, buscar por nombre y cargar el encabezado.
3. Reproducir con transportes/relojes falsos y capturar el orden y tiempo relativo.
4. Cancelar reproducción y comprobar que no quedan comandos pendientes.
5. Corromper encabezados, índices y límites; comprobar errores personalizados sin éxito parcial.

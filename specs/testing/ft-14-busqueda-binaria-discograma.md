---
id: ft-14
layer: testing
status: approved
---

# Pruebas de búsqueda binaria

## General description

- Validar búsqueda exacta, carga de encabezado y lectura limitada sin hardware ni SuperCollider.

## Inputs

- Índice temporal con nombres ordenados.
- Música temporal con encabezados y comandos.
- Directorios corruptos o truncados controlados.

## Outputs

- Resultado JUnit 5 y datos cargados o excepciones esperadas.

## Test criteria

- Nombre exacto se encuentra y devuelve su posición.
- Nombre ausente lanza `RecordingNotFoundException`.
- Índice con longitud incorrecta produce `RecordingException`.
- Encabezado equivocado, fuera de rango o truncado produce error propio.
- Los comandos cargados están dentro de inicio/fin exclusivos y conservan offsets/parámetros.
- Buscar no modifica ni reordena los archivos.
- `mvn test` usa `@TempDir` y no usa datos reales ni dispositivos.

## Assets

- Diagrama: `../diagrams/ft-14-busqueda-binaria-discograma.puml`.
- Reutilizar los tests existentes de repositorio y el fake de servicio.

## Implementation guidelines

- JUnit 5 en `src/test/java/powerband/repository/` y `src/test/java/powerband/service/`.
- Usar `@TempDir`, guardar varias grabaciones y abrir el repositorio nuevamente.
- Ejecutar `mvn test`.

## Business workflow

1. Crear índice ordenado temporal.
2. Buscar primero, medio y último nombre.
3. Buscar nombre inexistente.
4. Corromper tamaño o encabezado y verificar excepciones.
5. Verificar que comandos y origen permanezcan sin cambios.

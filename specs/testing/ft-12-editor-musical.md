---
id: ft-12
layer: testing
status: approved
---

# Pruebas del editor musical

## General description

- Verificar navegación, apariencia, línea de tiempo editable, grabación, carga, reproducción y creación de copias sin hardware ni SuperCollider.
- Cubrir servicio y repositorio con fakes o archivos temporales y comprobar que las grabaciones fuente no se alteren.

## Inputs

- Acciones Swing automatizadas o interacciones simuladas en modo headless.
- `RecordingService` y `RecordingRepository` falsos para pruebas UI.
- `BinaryRecordingRepository` y `@TempDir` para pruebas de persistencia.
- Secuencias de `SynthEvent` con diferentes tiempos, synths, acciones y parámetros.

## Outputs

- Estado visible de página, línea de tiempo, controles y mensajes de error.
- Resultado del guardado de una copia y contenido persistido de los archivos.

## Test criteria

- Las pruebas UI verifican las tres páginas, etiquetas, acciones disponibles y actualización de página sin perder estado cargado.
- La línea de tiempo refleja offsets y synths; editar offset, nota, duración, volumen o timbres cambia la copia de trabajo y no el objeto/origen original.
- La grabación comienza una sola vez; detenerla muestra el campo de nombre y conserva la captura ante duplicado o nombre inválido.
- Guardar una edición con nombre duplicado muestra un error y permite reintentar; un nombre único crea una copia independiente.
- Las acciones de carga y reproducción invocan el servicio esperado y los estados de error no se muestran como éxito.
- Las pruebas de repositorio confirman tamaños fijos, copia sin mutación de origen, índice ordenado en disco y listado actualizado cronológicamente.
- `mvn test` corre sin dispositivos físicos, servidor SuperCollider ni acceso a los archivos reales del usuario.

## Assets

- Diagrama de flujo: `../diagrams/ft-12-editor-musical.puml`.
- Convenciones: JUnit 5, clases `*Test.java`, fuentes bajo `src/test/java/powerband/` y archivos temporales con `@TempDir`.

## Implementation guidelines

- Escribir pruebas Swing en `src/test/java/powerband/ui/` y pruebas de servicio/repositorio junto a sus paquetes.
- Usar JUnit 5, dobles de prueba y `@TempDir`; no acceder a hardware ni a la carpeta `data/` real.
- Mantener el proyecto compilable con Java 25 y ejecutar `mvn test`.

## Business workflow

1. Probar visualización y navegación entre páginas con el servicio falso.
2. Cargar eventos de ejemplo y verificar presentación y edición de los campos soportados.
3. Simular grabación, detención, nombre duplicado, reintento y guardado exitoso.
4. Ejecutar pruebas de persistencia temporal y comparar contenido de origen y copia.
5. Ejecutar la suite JUnit completa y reportar fallos sin ocultarlos.

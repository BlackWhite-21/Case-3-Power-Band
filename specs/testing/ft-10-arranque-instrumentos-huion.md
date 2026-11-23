---
id: ft-10
layer: testing
status: approved
---

# Pruebas de arranque de instrumentos y hook Huion

## General description

- Validar la composición del arranque, aislamiento de fallos y ciclo de vida del callback sin requerir hardware en las pruebas unitarias.
- Mantener una prueba manual Windows separada para la Huion física; las pruebas simuladas no demuestran compatibilidad del dispositivo real.

## Inputs

- Fuentes Huion y micrófono falsas, cliente OSC sustituible y fallos simulados de inicialización.
- Para la prueba manual: Windows x64, controlador Huion y tableta conectada.

## Outputs

- Resultado de `mvn test`.
- Evidencia manual de inicialización del hook, entrega de presión y liberación de nota.

## Test criteria

- `mvn test` no carga `User32`, no abre el micrófono real y no necesita SuperCollider.
- La prueba de arranque confirma que el frame está visible antes de inicializar Huion.
- Un fallo simulado de Huion no impide iniciar el micrófono, y viceversa; una respuesta ausente de SuperCollider no cierra el editor.
- Una prueba del ciclo de callback confirma que el delegate entregado al hook sigue disponible después de que `attach` retorna y hasta que el cierre espera a todos los callbacks activos.
- La prueba de cierre confirma que el hook se desregistra antes de liberar la callback y que el cliente OSC permanece abierto hasta detener los instrumentos.
- La prueba manual en Windows confirma movimiento, presión variable, pen-up y cierre del hook. Registrar el hardware/controlador y el resultado; si no hay tableta disponible, marcar esta aceptación como pendiente, no como aprobada.

## Assets

- Secuencia de prueba manual y composición: `../diagrams/ft-10-arranque-instrumentos-huion.puml`.
- Contratos JNA existentes: `../backend/ft-9-huion-pointer-jna.md` y `../testing/ft-9-huion-pointer-jna.md`.

## Implementation guidelines

- Usar JUnit 5 en `src/test/java/powerband/`; reutilizar fakes existentes de OSC, Huion y audio.
- Mantener las pruebas unitarias independientes de Windows, la Huion y SuperCollider.
- Para verificar la retención de la callback, usar un backend nativo falso o una prueba determinista equivalente; no depender únicamente de llamadas no deterministas a `System.gc()`.
- Ejecutar `mvn test`; reportar por separado la prueba física en Windows.

## Business workflow

1. Ejecutar pruebas de composición con instrumentos falsos y verificar el orden visible/inicialización.
2. Simular fallos de dispositivos y timeout de SuperCollider; comprobar que los otros componentes continúan.
3. Verificar instalación, desregistro y retención/liberación del callback mediante un backend nativo falso.
4. En Windows con la Huion disponible, probar presión, movimiento, liberación y cierre; guardar la evidencia o dejar explícita la limitación si no hay hardware.

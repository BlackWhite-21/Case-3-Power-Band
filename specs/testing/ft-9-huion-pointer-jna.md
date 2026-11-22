---
id: ft-9
layer: testing
status: approved
---

# Pruebas del adaptador JNA Huion

## General description

- Aceptar o rechazar captura, normalización, coordenadas, threading y liberación de recursos sin requerir tableta ni servidor SuperCollider para las pruebas unitarias.
- Añadir una prueba de integración física separada, obligatoria para aprobar compatibilidad de la GS1331 y su controlador.

## Inputs

- Implementación falsa de `PointerEventSource` que emite lecturas de lápiz y permite simular errores sin invocar User32.
- Componente Swing de prueba con jerarquía, bounds y transformaciones DPI simuladas.
- En prueba manual/integración: Huion Kamvas 13 GS1331, controlador instalado y Windows 10/11.

## Outputs

- `DatosTableta` observados por oyentes y estado de recursos después del cierre.
- Evidencia de compatibilidad física que registra OS, versión/configuración del controlador, API seleccionada y valores de presión reportados.

## Test criteria

- Las pruebas de unidad cubren normalización y límites del rango Windows (`0..1024`), incluyendo presión cero en hover, máxima presión, dato de presión no disponible y rangos inválidos.
- Cada evento de entrada produce el estado esperado; `WM_POINTERDOWN` inicia contacto, `WM_POINTERUPDATE` publica movimiento, `WM_POINTERUP` libera y `WM_POINTERLEAVE`/pérdida de captura evita contacto obsoleto.
- Los eventos se convierten de coordenadas físicas a coordenadas locales Swing correctas con offsets de ventana/componente, escalado HiDPI y cambios de tamaño. Un evento fuera del componente no se informa como posición interna válida.
- Los oyentes se invocan fuera del EDT. Agregar/quitar oyentes durante la captura, una excepción de un oyente y cierre durante una lectura no bloquean el EDT ni dejan el hilo activo.
- Si Pointer API no reporta presión real, se espera un error explícito, no una lectura de mouse o presión inventada.
- Inicialización en componente no displayable, sistema/arquitectura incompatible y desconexión presentan errores/estado no disponible verificables.
- El cierre desregistra la ventana, finaliza worker/contexto y libera recursos una sola vez, incluso si se llama repetidamente o durante desconexión.
- `mvn test` corre sin hardware ni SuperCollider, y no compila código nativo propio. La fuente de eventos se sustituye con una implementación falsa en pruebas unitarias; la prueba con hardware se etiqueta como integración manual/Windows.
- La aceptación física verifica movimiento, contacto, variación real de presión y liberación en la GS1331. Windows 11 Home x64 (build 26300) con HUION HID 6.1.7600.16905 ya reportó `PT_PEN`, máscara de presión y valores variables 1..1023; registrar la ejecución/evidencia en la prueba. Completar la prueba física en Windows 10 antes de distribuir. Si Pointer API no entrega presión real en otra configuración objetivo, no habilitar el adaptador.

## Assets

- Contrato y diagrama de clases: `../backend/ft-9-huion-pointer-jna.md` y `../diagrams/ft-9-huion-pointer-jna.puml`.
- Convenciones: JUnit 5, clases de prueba `<ClassName>Test.java`, pruebas sin hardware físico en `src/test/java/`.

## Implementation guidelines

- Escribir pruebas Java en `src/test/java/powerband/instrument/`; ubicar fakes compartidos en `src/test/java/powerband/helpers/`.
- Probar `TabletaGrafica` contra una implementación inyectable de `PointerEventSource`; no instalar hooks ni acceder a User32 en las pruebas unitarias.
- Mantener pruebas de transformación de coordenadas y estados independientes del mapeo musical de `HuionInstrument`.
- Probar las conversiones de eventos con tests unitarios; evitar hooks y hardware en CI.
- Ejecutar `mvn test`. La prueba física debe reportarse por separado y nunca sustituirse por un test unitario.

## Business workflow

1. Inyectar un backend falso y emitir secuencias de eventos de lápiz.
2. Verificar estado, presión, coordenadas y despacho de listeners.
3. Simular errores, pérdida de captura, desconexión y cierre; comprobar que no queden recursos o contacto activo.
4. En una estación Windows con el modelo GS1331, repetir movimiento/presión/liberación mediante Windows Pointer API y guardar evidencia de compatibilidad.

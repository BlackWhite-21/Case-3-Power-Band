---
id: ft-10
layer: frontend
status: approved
---

# Arranque de instrumentos y hook Huion

## General description

- Mostrar el editor Swing y enlazar su componente visible con la captura Huion.
- Mantener la ventana operativa cuando SuperCollider o un instrumento no esté disponible; informar el fallo sin impedir iniciar los demás instrumentos.

## Inputs

- Inicio de `powerband.Main`.
- Ventana `RecordingEditorFrame` y su componente de contenido.
- Errores de inicialización de Huion, micrófono y SuperCollider.

## Outputs

- Editor visible antes de inicializar la tableta.
- Avisos de disponibilidad de SuperCollider y errores de instrumento mostrados al usuario.

## Test criteria

- El componente de captura es displayable antes de inicializar `TabletaHuionInputSource`.
- La falta de respuesta de SuperCollider muestra un aviso y no cierra el editor.
- Un fallo de Huion no impide intentar iniciar el micrófono; un fallo del micrófono no impide intentar iniciar Huion.
- Al cerrar la ventana se solicita detener los instrumentos y liberar el cliente OSC.

## Assets

- Secuencia de arranque y cierre, compartida con `../backend/ft-10-arranque-instrumentos-huion.md`:

```plantuml
@startuml ft-10-arranque-instrumentos-huion
title ft-10 - Arranque de instrumentos y ciclo de vida del hook Huion
hide footbox

actor Usuario
participant Main
participant RecordingEditorFrame
participant HuionInstrument
participant TabletaHuionInputSource
participant TabletaGrafica
participant JnaPointerInputSource
participant WindowsPointerAPI
participant MicrophoneInstrument
participant OscClient

Usuario -> Main: main(args)
Main -> RecordingEditorFrame: crear y setVisible()
par Iniciar Huion fuera del EDT
  Main -> HuionInstrument: start()
  HuionInstrument -> TabletaHuionInputSource: start(consumidores)
  TabletaHuionInputSource -> TabletaGrafica: initialize(componente visible)
  TabletaGrafica -> JnaPointerInputSource: attach(componente) en EDT
  JnaPointerInputSource -> WindowsPointerAPI: SetWindowsHookExW(callback)
  JnaPointerInputSource -> JnaPointerInputSource: retener callback fuertemente
else Iniciar micrófono
  Main -> MicrophoneInstrument: start()
else Verificar SuperCollider
  Main -> OscClient: ping()
end

WindowsPointerAPI -> JnaPointerInputSource: eventos WM_POINTER
JnaPointerInputSource -> TabletaGrafica: encolar lectura
TabletaGrafica -> HuionInstrument: publicar lectura fuera del EDT

Usuario -> RecordingEditorFrame: cerrar ventana
Main -> HuionInstrument: stop()
TabletaGrafica -> JnaPointerInputSource: detach()
JnaPointerInputSource -> WindowsPointerAPI: UnhookWindowsHookEx()
JnaPointerInputSource -> JnaPointerInputSource: esperar callbacks activos
JnaPointerInputSource -> JnaPointerInputSource: liberar callback retenido
Main -> MicrophoneInstrument: stop()
Main -> OscClient: close()
@enduml
```

## Implementation guidelines

- Usar Java 25+, Maven y Swing.
- Mantener la composición de arranque en `src/main/java/powerband/Main.java` y la ventana en `src/main/java/powerband/ui/`.
- Mostrar el frame antes de iniciar la captura Huion; no bloquear el EDT con inicialización de dispositivos o ping OSC.
- Mantener los mensajes y valores configurables en `src/main/java/powerband/util/IConstants.java`.

## Business workflow

1. Construir el servicio de grabación, el cliente OSC y los instrumentos disponibles.
2. Mostrar el editor para que la superficie asociada a Huion tenga peer nativo.
3. Iniciar Huion, micrófono y verificación de SuperCollider sin que un fallo individual cancele los demás.
4. Reportar errores en la interfaz y mantener el editor abierto.
5. Al cerrar, detener instrumentos y cerrar el cliente OSC.

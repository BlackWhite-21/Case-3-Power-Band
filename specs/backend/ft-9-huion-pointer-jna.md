---
id: ft-9
layer: backend
status: approved
---

# Adaptador JNA para tableta Huion

## General description

- Agregar captura asíncrona del lápiz de la Huion Kamvas 13 GS1331 para Windows 10/11 y su consumo desde componentes Swing, usando JNA para invocar directamente Windows API.
- Exponer `TabletaGrafica`, `DatosTableta` y `TabletaListener`, manteniendo la captura separada del dibujo y de `HuionInstrument`.
- Usar Windows Pointer API cuando el dispositivo y su controlador entreguen eventos `PT_PEN` y presión real. La configuración GS1331/driver probada ya cumple esta condición; las configuraciones que no reporten presión fallan explícitamente.
- No agregar métodos JNI, código C++ ni bibliotecas nativas propias; usar las dependencias Java de JNA.
- Bloquear la implementación con un resultado explícito de compatibilidad si ninguna API entrega datos reales. No usar eventos de mouse ni sintetizar presión.

## Inputs

- Un `Component` Swing visible y con peer nativo, asociado como superficie activa de captura.
- Eventos de lápiz del dispositivo y capacidades de presión reportadas por el backend nativo.
- Oyentes `TabletaListener` registrados por el consumidor.

## Outputs

- `DatosTableta` inmutables con `x` e `y` relativos al componente en unidades lógicas de Swing, presión real normalizada a `[0.0, 1.0]` y estado `PRESIONADO`, `MOVIMIENTO`, `LIBERADO` o `FUERA_SUPERFICIE`.
- Notificaciones a oyentes desde un hilo de trabajo, nunca desde el EDT ni desde el callback JNA del hook de mensajes.
- Errores explícitos de inicialización, API/controlador no compatible, desconexión y cierre mediante excepciones propias del proyecto.

## Test criteria

- En el GS1331 con el controlador/versiones registrados en la prueba, la verificación detecta `PT_PEN` y cambios de presión reales. Windows Pointer API debe reportar disponibilidad de presión (`PEN_MASK_PRESSURE`) antes de aceptarse como backend.
- En la configuración GS1331/driver probada, Pointer API reporta presión real y no se necesita Wintab. Si otra configuración no reporta presión, el adaptador falla con un error claro; no reemplaza la presión ausente por cero ni por un valor derivado del mouse.
- Los eventos `WM_POINTERDOWN`, `WM_POINTERUPDATE`, `WM_POINTERUP`, `WM_POINTERLEAVE` y pérdida de captura se traducen a los cuatro estados documentados. `WM_POINTERUPDATE` puede representar movimiento con o sin contacto; presión cero por hover se distingue de presión no soportada.
- La presión usa el rango documentado por Windows Pointer API, valida que el valor esté entre `0` y `1024` y lo normaliza a `[0.0, 1.0]`. Un dato fuera de rango se reporta como error. Liberación y salida del área activa publican presión cero.
- Las coordenadas físicas de pantalla se convierten al origen del componente Swing, incluyendo escalado DPI y componentes anidados. Las pruebas cubren monitor al 100%, monitor HiDPI y coordenadas fuera del componente.
- El consumidor puede registrar y retirar oyentes; cada evento se entrega una sola vez por oyente. Una excepción de un oyente no termina silenciosamente el hilo de captura ni impide notificar los demás.
- La inicialización requiere que el componente esté displayable; una inicialización inválida, una DLL ausente o un backend no compatible produce un error reportable.
- `close()` solicita el fin de la captura, desregistra el hook fuera del EDT y termina el hilo. El cierre repetido es seguro y no deja eventos activos.
- La captura y el cierre no esperan operaciones de dispositivo en el EDT; ningún `TabletaListener` se ejecuta en el EDT.
- Desconexión, pérdida de captura y salida del área activa apagan una lectura en contacto antes de terminar el flujo.

## Assets

- Contrato de clases:

```plantuml
@startuml ft-9-huion-pointer-jna
title ft-9 - Adaptador JNA para tableta Huion en Swing
skinparam classAttributeIconSize 0
hide empty members

class TabletaGrafica {
  - component : Component
  - eventSource : JnaPointerInputSource
  - listeners : List<TabletaListener>
  + initialize(pComponent : Component) : CompletableFuture
  + addListener(pListener : TabletaListener)
  + removeListener(pListener : TabletaListener)
  + closeAsync() : CompletableFuture
  + close()
}

record DatosTableta {
  + x : double
  + y : double
  + pressure : float
  + estado : EstadoPluma
}

enum EstadoPluma {
  PRESIONADO
  MOVIMIENTO
  LIBERADO
  FUERA_SUPERFICIE
}

interface TabletaListener {
  + onDatosTableta(pDatos : DatosTableta)
}

class JnaPointerInputSource <<JNA>> {
  + attach(pComponent : Component)
  + pollEvent(pTimeoutMs : int) : DatosTableta
  + requestStop()
  + detach()
}

interface WindowsPointerApi <<JNA>> {
  + GetPointerType(pPointerId, pType)
  + GetPointerPenInfo(pPointerId, pPenInfo)
  + SetWindowsHookExW(pHookType, pHookProc, pModule, pThreadId)
  + UnhookWindowsHookEx(pHook)
}

TabletaGrafica --> JnaPointerInputSource
TabletaGrafica --> TabletaListener
TabletaGrafica ..> DatosTableta : publica
DatosTableta --> EstadoPluma
JnaPointerInputSource --> WindowsPointerApi
JnaPointerInputSource ..> NativePointerEvent : encola

note right of TabletaGrafica
  Los eventos se despachan en un hilo de trabajo,
  nunca desde el EDT ni desde el callback JNA del hook.
end note
@enduml
```

- Referencias verificadas:
  - Microsoft, [`GetPointerPenInfo`](https://learn.microsoft.com/en-us/windows/win32/api/winuser/nf-winuser-getpointerpeninfo) y [`POINTER_PEN_INFO`](https://learn.microsoft.com/en-us/windows/win32/api/winuser/ns-winuser-pointer_pen_info): la API obtiene datos para punteros de lápiz; la presión depende de que el dispositivo la reporte.
  - Microsoft, [`WM_POINTERLEAVE`](https://learn.microsoft.com/en-us/windows/win32/inputmsg/wm-pointerleave): documenta salida de límites/detección y advierte que no se debe asumir emparejamiento simple de mensajes; también destaca DPI awareness.
  - JNA, [`Native.getComponentID(Component)`](https://javadoc.io/doc/net.java.dev.jna/jna/5.17.0/com/sun/jna/Native.html): obtiene el identificador nativo del componente Swing displayable.
  - Huion, [Kamvas 13](https://www.huion.com/products/kamvas-13): identifica el modelo GS1331 y las especificaciones del lápiz. La ficha del producto no confirma que el controlador exponga presión mediante Windows Pointer API.
- Diagnóstico previo del dispositivo/API en este equipo: Windows 11 Home x64, build 26300, driver HUION HID 6.1.7600.16905. Se observaron `PT_PEN`, `PEN_MASK_PRESSURE` (`penMask=0xD`), mensajes DOWN/UPDATE/UP y presión variable de 1 a 1023. `RegisterPointerInputTarget` devolvió acceso denegado, aunque la ventana recibió los mensajes por el enrutamiento normal; la implementación no debe depender de esa llamada. Este diagnóstico no valida todavía la integración del hook JNA descrita aquí.
- Wintab no se necesita si la integración JNA confirma la presión real en esta configuración. La prueba física del hook en Windows 11 y la validación en Windows 10 siguen pendientes antes de distribuir la feature. Si Pointer API no entrega presión real, el adaptador debe quedar no disponible.
- El diagrama se guarda en `specs/diagrams/ft-9-huion-pointer-jna.puml`.

## Implementation guidelines

- Java 25+, Maven y JNA; usar `Native.getComponentID` de JNA para obtener el HWND del `Window` Swing. No depender de APIs internas/reflexión de `sun.awt`.
- Ubicar la API Java en `src/main/java/powerband/instrument/`, `DatosTableta` en `src/main/java/powerband/dto/`, excepciones en `src/main/java/powerband/exception/` y constantes Java en `src/main/java/powerband/util/IConstants.java`.
- No agregar `src/main/native`, JNI, C++ ni DLL propia. Declarar las llamadas a User32 y las estructuras Windows con JNA.
- Instalar un hook de mensajes de hilo con JNA y filtrar `WM_POINTER*` del HWND Swing asociado. El callback captura datos y una instantánea breve de geometría Swing en el EDT, los encola y deja el mapeo final y las notificaciones al worker Java.
- Mantener el EDT libre de espera, polling y cierre bloqueante.
- Convertir coordenadas físicas del puntero a coordenadas locales lógicas del `Component`, contemplando DPI, cambios de monitor, jerarquía Swing y tamaño/origen actual del componente.
- Diferenciar presión legítima cero (sin contacto/hover) de presión no reportada. Usar el rango documentado por Windows Pointer API (`0..1024`) y validar el indicador de presión antes de publicar datos.
- Usar `IConstants` para valores Java; no se mantiene un header ni una DLL propios. Seguir las convenciones del proyecto: paquetes minúsculos, parámetros con prefijo `p`, encapsulamiento y excepciones explícitas.
- Si la lectura física se integra a `HuionInstrument`, mantener el mapeo existente en `TabletReading`/`HuionInputMapper`; este adaptador no dibuja ni decide notas o parámetros del saxofón.

## Business workflow

1. Validar que el `Component` esté displayable y obtener el HWND de su ventana mediante `Native.getComponentID`.
2. Consultar los eventos del dispositivo/controlador; aceptar Pointer API solo cuando entregue `PT_PEN` y presión real.
3. Si Pointer API no proporciona presión real, informar el error y no iniciar una captura simulada.
4. Enrutar los mensajes nativos a una cola y convertir cada dato en coordenadas del componente, presión normalizada y estado.
5. Notificar los oyentes fuera del EDT; el consumidor Swing programa por sí mismo cualquier cambio de UI con `SwingUtilities.invokeLater`.
6. Al salir del área, liberar el contacto activo; al cerrar o desconectar, detener hilos, retirar hooks y liberar todos los recursos nativos.

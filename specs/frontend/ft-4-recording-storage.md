---
id: ft-4
layer: frontend
status: approved
---

# Guardado de grabaciones del editor musical

## General description

- La interfaz Swing inicia y termina la captura de eventos de todos los instrumentos.
- Al terminar, solicita un nombre y vuelve a solicitarlo si está vacío, inválido o duplicado.
- Muestra el estado de grabación, la confirmación de guardado y los errores de persistencia.

## Inputs

- Acciones del usuario: iniciar y terminar grabación.
- Eventos `SynthEvent` notificados por los instrumentos al editor.
- `RecordingInfo` y excepciones entregados por el servicio.

## Outputs

- Llamadas a `RecordingService` para iniciar, detener y guardar una grabación.
- Indicador visual de captura activa y mensajes de error o confirmación.

## Test criteria

- Iniciar cambia la UI a estado de grabación y solicita iniciar el servicio una vez.
- Terminar detiene la captura antes de solicitar el nombre.
- Un nombre inválido o repetido conserva la grabación pendiente y permite intentar otro nombre.
- Un guardado exitoso muestra el nombre y vuelve al estado inactivo.
- Los errores de E/S se muestran al usuario; no se presentan como guardado exitoso.

## Assets

```plantuml
@startuml
actor Usuario
participant "Editor Swing" as UI
participant "Instrumentos" as Instruments
participant RecordingService as Service
participant RecordingRepository as Repo
database "recordings.music" as Music
database "recordings.index" as Index
collections "recordings.list" as List
participant BalancedMergeSort as Sort

Usuario -> UI: Activar grabación
UI -> Service: startRecording()
Service -> Repo: beginRecording(startedAt)
Repo -> Music: Escribir encabezado provisional

loop Por cada SynthEvent recibido
    Instruments -> Service: onSynthEvent(event)
    Service -> Service: Calcular offset desde el inicio
    Service -> Repo: appendEvent(event con offset)
    Repo -> Music: Escribir comando binario fijo
end

Usuario -> UI: Terminar grabación
UI -> Service: stopRecording()
Service -> Repo: Cerrar captura pendiente
UI -> Usuario: Solicitar nombre

loop Hasta que el nombre sea válido y único
    Usuario --> UI: nombre
    UI -> Service: saveRecording(name)
    Service -> Service: Validar nombre
    alt Nombre inválido
        Service --> UI: InvalidNameException
        UI -> Usuario: Solicitar otro nombre
    else Nombre válido
        Service -> Repo: findByName(name)
        Repo -> Index: Búsqueda binaria en disco
        alt Nombre duplicado
            Repo --> Service: DuplicateNameException
            Service --> UI: DuplicateNameException
            UI -> Usuario: Solicitar otro nombre
        else Nombre disponible
            Repo --> Service: Posición no encontrada
        end
    end
end

Service -> Repo: finalizeRecording(name, endOffset)
Repo -> Music: Actualizar encabezado fijo
Repo -> Index: Añadir nombre y posición del encabezado
Repo -> Sort: Ordenar índice en disco
Sort -> Index: Reemplazar por índice ordenado
Repo -> List: Añadir nombre y fecha/hora
Service --> UI: RecordingInfo
UI --> Usuario: Confirmar guardado
@enduml
```

## Implementation guidelines

- Java 25+, Maven y Swing; usar las constantes de `src/main/java/powerband/util/IConstants.java`.
- Integrar la ventana/editor en `src/main/java/powerband/ui/`; delegar captura y persistencia a `src/main/java/powerband/service/`.
- Mantener los eventos de los instrumentos (`src/main/java/powerband/instrument/`) conectados al editor mientras la grabación está activa.
- Aplicar las convenciones del proyecto: parámetros con prefijo `p`, campos encapsulados y excepciones explícitas.

## Business workflow

1. El usuario activa la grabación; la UI actualiza su estado y llama al servicio.
2. Mientras se graba, el editor acepta eventos de todos los instrumentos.
3. Al terminar, la UI detiene la captura y solicita el nombre.
4. Si el servicio rechaza el nombre por inválido o repetido, la UI solicita otro sin descartar la captura pendiente.
5. Al confirmar el guardado, la UI presenta el resultado y vuelve al estado inactivo.
